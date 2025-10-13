pipeline {
  agent any

  environment {
    APP_NAME     = "books-api"
    IMAGE_BASE   = "chico0706/books-api"
    SONARQUBE_ENV = "MySonarServer"
    SONAR_HOST   = "http://localhost:9000"
    SONAR_TOKEN  = credentials('SONAR_TOKEN')
  }

  stages {

    /* 🔹 Common stages for all branches */
    stage('Build & Unit Test') {
      steps {
        echo "🔨 Building and running unit tests..."
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
        echo "🔎 Running static code analysis..."
        withSonarQubeEnv("${SONARQUBE_ENV}") {
          sh """
            mvn sonar:sonar \
              -Dsonar.projectKey=ODSOFT \
              -Dsonar.host.url=${SONAR_HOST} \
              -Dsonar.login=${SONAR_TOKEN} \
              -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
          """
        }
      }
    }

    stage('Integration Tests') {
      steps {
        echo "🧪 Running integration tests..."
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

    /* 🔹 Build Image for all branches */
    stage('Build Docker Image') {
      steps {
        script {
          def imageTag = env.BRANCH_NAME == 'main' ? 'latest' : env.BRANCH_NAME
          env.IMAGE_NAME = "${IMAGE_BASE}:${imageTag}"
          echo "🛠️ Building image ${IMAGE_NAME}"
          sh "docker build -t ${IMAGE_NAME} ."
        }
      }
    }

    /* 🔹 Push image only for staging & prod */
    stage('Push Docker Image') {
      when {
        anyOf { branch 'staging'; branch 'main' }
      }
      steps {
        script {
          echo "🪣 Pushing image ${IMAGE_NAME}"
          withCredentials([usernamePassword(credentialsId: 'docker-creds',
                                            usernameVariable: 'DOCKER_USER',
                                            passwordVariable: 'DOCKER_PASS')]) {
            sh """
              echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
              docker push ${IMAGE_NAME}
            """
          }
        }
      }
    }

    /* 🔹 DEV environment */
    stage('Deploy to Dev') {
      when { branch 'develop' }
      steps {
        echo "🚀 Deploying to Dev environment..."
        sh "docker-compose -f docker-compose.dev.yml up -d --build"
      }
    }

    /* 🔹 STAGING environment */
    stage('Deploy to Staging') {
      when { branch 'staging' }
      steps {
        echo "🚀 Deploying to Staging environment..."
        sh "docker-compose -f docker-compose.staging.yml up -d --build"
      }
    }

    /* 🔹 PRODUCTION environment */
    stage('Promote to Production') {
      when { branch 'main' }
      steps {
        input message: "Deploy to production?", ok: "Deploy"
        echo "🚀 Deploying to Production environment..."
        sh "docker-compose -f docker-compose.prod.yml up -d --build"
      }
    }

  } // end stages

  post {
    success {
      echo "✅ Pipeline for branch '${env.BRANCH_NAME}' completed successfully!"
    }
    failure {
      echo "❌ Pipeline for branch '${env.BRANCH_NAME}' failed!"
    }
  }
}
