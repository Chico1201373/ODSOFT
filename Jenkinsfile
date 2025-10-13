pipeline {
  agent any

  options {
    buildDiscarder(logRotator(daysToKeepStr: '30', numToKeepStr: '50'))
    timestamps()
    ansiColor('xterm')
    disableConcurrentBuilds()
  }

  // Request tools from Jenkins global config. Ensure you have a JDK 21 installation
  // named 'jdk21' and a Maven installation named 'maven-3.9.6' (or update names as configured).
  tools {
    jdk 'jdk21'
    maven 'maven-3.9.6'
  }

  environment {
    APP_NAME    = 'books-api'
    // Docker registry settings (override with Jenkins credentials/vars)
    REGISTRY    = credentials('docker-registry-url') ?: 'docker.io'
    DOCKER_CREDENTIALS = 'docker-creds'

    // SonarQube
    SONARQUBE_ENV = 'MySonarServer'
    SONAR_TOKEN = credentials('SONAR_TOKEN')

    // Default image repo base (override via pipeline parameter or env var)
    IMAGE_BASE = "chico0706/${APP_NAME}"
  }

  parameters {
    booleanParam(name: 'PROMOTE_TO_PROD', defaultValue: false, description: 'When true and on staging, promote the built image to production tag')
    string(name: 'IMAGE_TAG', defaultValue: '', description: 'Optional override for image tag (defaults to branch or build number)')
  }

  stages {

    stage('Prepare') {
      steps {
        script {
          // detect tag
          def defaultTag = env.BRANCH_NAME == 'main' ? 'latest' : (env.BRANCH_NAME ?: "build-${env.BUILD_NUMBER}")
          env.TAG = params.IMAGE_TAG ? params.IMAGE_TAG : defaultTag
          env.IMAGE_NAME = "${IMAGE_BASE}:${env.TAG}"
          echo "Building image ${env.IMAGE_NAME}"
        }
      }
    }

    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Build - Unit Tests') {
      steps {
        // Use Maven if pom.xml exists, otherwise try Gradle
        script {
          if (fileExists('pom.xml')) {
            sh 'mvn -B -V -DskipTests=false clean verify'
          } else if (fileExists('build.gradle')) {
            sh './gradlew clean build --no-daemon'
          } else {
            error 'No recognized build file (pom.xml or build.gradle) found.'
          }
        }
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: '**/target/surefire-reports/TEST-*.xml,**/build/test-results/**/*.xml'
          archiveArtifacts artifacts: '**/target/*.jar,**/build/libs/*.jar', allowEmptyArchive: true
        }
      }
    }

    stage('Static Analysis - SonarQube') {
      when { expression { fileExists('pom.xml') || fileExists('build.gradle') } }
      steps {
        withSonarQubeEnv(SONARQUBE_ENV) {
          withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
            script {
              if (fileExists('pom.xml')) {
                sh "mvn sonar:sonar -Dsonar.login=$SONAR_TOKEN"
              } else {
                sh "./gradlew sonarqube -Dsonar.login=$SONAR_TOKEN"
              }
            }
          }
        }
      }
    }

    stage('Integration Tests') {
      steps {
        script {
          if (fileExists('pom.xml')) {
            sh 'mvn -B verify -DskipUnitTests=true'
          } else if (fileExists('build.gradle')) {
            sh './gradlew integrationTest || true'
          } else {
            echo 'Skipping integration tests (no build file)'
          }
        }
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: '**/target/failsafe-reports/*.xml,**/build/**/reports/**/integrationTest/*.xml'
        }
      }
    }

    stage('Build Docker Image') {
      steps {
        script {
          sh "docker build -t ${env.IMAGE_NAME} ."
        }
      }
    }

    stage('Push Image (Staging/Main)') {
      when {
        anyOf {
          branch 'staging'
          branch 'main'
        }
      }
      steps {
        withCredentials([usernamePassword(credentialsId: DOCKER_CREDENTIALS, usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
          sh '''
            echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin ${REGISTRY}
            docker push ${IMAGE_NAME}
          '''
        }
      }
    }

    stage('Deploy to Dev') {
      when { branch 'develop' }
      steps {
        echo 'Deploying to Dev environment (dev branch)'
        // example: trigger remote deploy job or run compose locally if available
        sh 'docker-compose -f docker-compose.dev.yml up -d --build || true'
      }
    }

    stage('Deploy to Staging') {
      when { branch 'staging' }
      steps {
        echo 'Deploying to Staging environment (staging branch)'
        sh 'docker-compose -f docker-compose.staging.yml up -d --build || true'
      }
    }

    stage('Promotion to Production') {
      when { branch 'staging' }
      steps {
        script {
          if (params.PROMOTE_TO_PROD) {
            // Tag the staging image as prod and push
            def prodTag = "${IMAGE_BASE}:prod"
            withCredentials([usernamePassword(credentialsId: DOCKER_CREDENTIALS, usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
              sh '''
                echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin ${REGISTRY}
                docker tag ${IMAGE_NAME} ${IMAGE_BASE}:prod
                docker push ${IMAGE_BASE}:prod
              '''
            }
            echo "Promoted ${env.IMAGE_NAME} to ${IMAGE_BASE}:prod"
          } else {
            echo 'Promotion to production skipped (PROMOTE_TO_PROD is false)'
          }
        }
      }
    }

    stage('Deploy to Production') {
      when { branch 'main' }
      steps {
        input message: 'Approve deployment to PRODUCTION?', ok: 'Deploy'
        echo 'Deploying to Production environment (main branch)'
        sh 'docker-compose -f docker-compose.prod.yml up -d --build || true'
      }
    }
  }

  post {
    success {
      echo "Pipeline completed: ${env.BRANCH_NAME}"
    }
    failure {
      echo "Pipeline failed: ${env.BRANCH_NAME}"
    }
    unstable {
      echo 'Pipeline finished unstable'
    }
  }
}