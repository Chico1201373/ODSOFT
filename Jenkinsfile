pipeline {
  agent any

  environment {
    APP_NAME   = "books-api"
    IMAGE_NAME = "odsoft/books-api"
    TAG        = "${env.GIT_COMMIT ?: 'local'}"
    SONAR_HOST = "http://localhost:9000" 
  }

  options {
    timestamps()
    buildDiscarder(logRotator(daysToKeepStr: '14'))
  }

  stages {

    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Build & Compile') {
      steps {
        sh "mvn -B -DskipTests clean package"
      }
      post {
        success {
          archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
        }
      }
    }

    stage('Static Analysis') {
  steps {
    withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
      sh "mvn sonar:sonar -Dsonar.host.url=${SONAR_HOST} -Dsonar.login=${SONAR_TOKEN}"
    }
  }
}


    stage('Unit Tests') {
    steps {
        sh "mvn -B test"
        sh "ls -l target/surefire-reports/" 
    }
    post {
        always {
            junit '**/target/surefire-reports/TEST-*.xml'
        }
    }
    }

    stage('Mutation Tests (PITest)') {
      steps {
        sh "mvn org.pitest:pitest-maven:mutationCoverage"
      }
      post {
        always {
          archiveArtifacts artifacts: 'target/pit-reports/**', fingerprint: true
        }
      }
    }

    stage('Integration Tests') {
    steps {
        sh "mvn -B verify -DskipUnitTests=true"
        sh "ls -l target/failsafe-reports/"
    }
    post {
        always {
            junit '**/target/failsafe-reports/*.xml'
        }
    }
}
    stage('Build Docker Image') {
      steps {
        sh "docker build -t ${IMAGE_NAME}:${TAG} ."
      }
    }

    stage('Push Image') {
      when {
        expression { env.BRANCH_NAME == 'main' }
      }
      steps {
        withCredentials([usernamePassword(credentialsId: 'docker-creds', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
          sh """
            echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin
            docker push ${IMAGE_NAME}:${TAG}
          """
        }
      }
    }

    stage('Deploy to Dev') {
      when { branch 'develop' }
      steps {
        sh """
          export IMAGE_TAG=${TAG}
          docker-compose -f docker-compose.dev.yml up -d --build
        """
      }
    }

    stage('Functional/System Tests') {
      steps {
        sh "mvn -Dkarate.env=dev test -Dkarate.options='--tags @smoke'"
      }
    }

    stage('Promote to Staging/Prod') {
      when { branch 'main' }
      steps {
        script {
          input(message: "Promote to staging?")
        }
        sh "docker-compose -f docker-compose.staging.yml up -d --build"
      }
    }
  }

  post {
    always {
      archiveArtifacts artifacts: 'target/*.jar, target/*.war', allowEmptyArchive: true
    }
    success {
      echo "✅ Pipeline succeeded!"
    }
    failure {
  echo "Build failed — email disabled (no SMTP configured)"
  }
  }
}
// 