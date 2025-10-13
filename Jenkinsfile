pipeline {
  agent any

  environment {
    APP_NAME   = "books-api"
  IMAGE_NAME = 'odsoft/books-api'
  TAG = "${env.BRANCH_NAME == 'main' ? 'latest' : env.BRANCH_NAME}"


    SONAR_HOST = "http://localhost:9000"
    SONARQUBE_ENV = 'MySonarServer'
    SONAR_TOKEN = credentials('SONAR_TOKEN')

  }

  stages {

    stage('Build & Unit Test') {
      steps {
        sh 'mvn -B clean package'
        sh 'mvn test'
      }
      post {
        always {
          junit '**/target/surefire-reports/TEST-*.xml'
        }
      }
    }

    stage('Static Code Analysis (SonarQube)') {
  steps {
    withSonarQubeEnv('MySonarServer') {
      withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
        sh '''
          mvn sonar:sonar \
            -Dsonar.projectKey=ODSOFT \
            -Dsonar.host.url=$SONAR_HOST_URL \
            -Dsonar.login=$SONAR_TOKEN \
            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
        '''
      }
    }
  }
}

    stage('Integration Tests') {
      steps {
        sh 'mvn verify -DskipUnitTests=true'
      }
      post {
        always {
            script {
                if (fileExists('target/failsafe-reports')) {
                    junit 'target/failsafe-reports/*.xml'
                } else {
                    echo "No Failsafe reports found, skipping."
                }
            }
        }
    }
    }

    stage('Build Docker Image') {
      steps {
        script {
          env.IMAGE_NAME = "${IMAGE_BASE}:${env.BRANCH_NAME}-${TAG}"
          sh "docker build -t ${IMAGE_NAME} ."
        }
      }
    }

  
 stage('Push Docker Image') {
  when {
    anyOf {
      branch 'staging'
      branch 'main'
      branch 'develop'
    }
  }
  steps {
    script {
      echo "🪣 Pushing image: ${IMAGE_NAME}:${TAG}"
    }
    withCredentials([usernamePassword(credentialsId: 'docker-creds',
                                      usernameVariable: 'DOCKER_USER',
                                      passwordVariable: 'DOCKER_PASS')]) {
      sh '''
        echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
        docker push ${IMAGE_NAME}:${TAG}
      '''
    }
  }
}




    stage('Deploy to Dev') {
      when { branch 'develop' }
      steps {
        sh """
          echo "🚀 Deploying to Dev environment"
          docker-compose -f docker-compose.dev.yml up -d --build
        """
      }
    }

    stage('Deploy to Staging') {
      when { branch 'staging' }
      steps {
        sh """
          echo "🚀 Deploying to Staging environment"
          docker-compose -f docker-compose.staging.yml up -d --build
        """
      }
    }

    stage('Promote to Production') {
      when { branch 'main' }
      steps {
        input message: "Deploy to production?", ok: "Deploy"
        sh """
          echo "🚀 Deploying to Production environment"
          docker-compose -f docker-compose.prod.yml up -d --build
        """
      }
    }
  }

  post {
    success {
      echo "✅ ${env.BRANCH_NAME} pipeline completed successfully"
    }
    failure {
      echo "❌ ${env.BRANCH_NAME} pipeline failed"
    }
  }
}
