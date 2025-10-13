pipeline {
  agent any

  options {
    buildDiscarder(logRotator(daysToKeepStr: '30', numToKeepStr: '50'))
    timestamps()
    disableConcurrentBuilds()
  }

  environment {
    APP_NAME    = 'books-api'
    SONAR_TOKEN = credentials('SONAR_TOKEN') // Jenkins credential ID

    // Docker registry settings (override with Jenkins credentials/vars)
    // Default registry; override by editing this file or setting env in Jenkins job
  DOCKER_CREDENTIALS = 'docker-creds'
    // Default image repo base (override via pipeline parameter or env var)
    IMAGE_BASE = "chico0706/${APP_NAME}"
    PROMOTE_TO_PROD = 'false'
    IMAGE_TAG = ''
    SONAR_SERVER_NAME = 'MySonarServer'
  }

  stages {

    stage('Prepare') {
      steps {
        script {
          // detect tag
          def defaultTag = env.BRANCH_NAME == 'main' ? 'latest' : (env.BRANCH_NAME ?: "build-${env.BUILD_NUMBER}")
          env.TAG = env.IMAGE_TAG?.trim() ? env.IMAGE_TAG : defaultTag
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
        script {
          if (!fileExists('pom.xml')) {
            error 'Maven build required: pom.xml not found.'
          }
          // Maven build and tests
          sh 'mvn -B -V -DskipTests=false clean verify'
          // generate JaCoCo XML report (if plugin present in pom)
          sh 'mvn jacoco:report || true'
          // detect common jacoco xml paths for Maven
          if (fileExists('target/site/jacoco/jacoco.xml')) {
            env.JACOCO_PATH = 'target/site/jacoco/jacoco.xml'
          } else if (fileExists('target/jacoco.xml')) {
            env.JACOCO_PATH = 'target/jacoco.xml'
          } else {
            echo 'No JaCoCo XML found in target/; Sonar coverage may be unavailable.'
          }
        }
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: '**/target/surefire-reports/TEST-*.xml,**/build/test-results/**/*.xml'
          archiveArtifacts artifacts: '**/target/*.jar,**/build/libs/*.jar', allowEmptyArchive: true
          // Archive jacoco xml if found
          script {
            if (env.JACOCO_PATH) {
              echo "Archiving JaCoCo report at: ${env.JACOCO_PATH}"
              archiveArtifacts artifacts: "${env.JACOCO_PATH}", allowEmptyArchive: false
            } else {
              echo 'No JaCoCo report to archive.'
            }
          }
        }
      }
    }

    stage('SonarQube Analysis') {
      steps {
        script {
          withSonarQubeEnv(env.SONAR_SERVER_NAME) {
            try {
              echo "SONAR_SERVER_NAME=${env.SONAR_SERVER_NAME}"
              echo "SONAR_HOST_URL=${env.SONAR_HOST_URL}"

              def coverageArg = ''
              if (env.JACOCO_PATH) {
                coverageArg = "-Dsonar.coverage.jacoco.xmlReportPaths=${env.JACOCO_PATH}"
                echo "Using JaCoCo report: ${env.JACOCO_PATH}"
              } else {
                echo 'No JaCoCo report detected; continuing without coverage property.'
              }

              def scannerCmd = "mvn sonar:sonar -Dsonar.projectKey=my-project -Dsonar.host.url=${env.SONAR_HOST_URL} -Dsonar.login=${SONAR_TOKEN} -Dsonar.java.coveragePlugin=jacoco ${coverageArg}"
              echo "Running: ${scannerCmd}"
              def rc = sh(script: scannerCmd, returnStatus: true)
              if (rc != 0) {
                echo "Sonar scanner returned non-zero exit code: ${rc}. Skipping waitForQualityGate."
                currentBuild.result = 'UNSTABLE'
              } else {
                // Wait for quality gate result (requires SonarQube plugin + webhook)
                try {
                  timeout(time: 5, unit: 'MINUTES') {
                    def qg = waitForQualityGate(abortPipeline: false)
                    if (qg == null) {
                      echo 'No quality gate result available.'
                    } else {
                      echo "Sonar Quality Gate status: ${qg.status}"
                      if (qg.status != 'OK') {
                        unstable "Quality gate not OK: ${qg.status}"
                      }
                    }
                  }
                } catch (err) {
                  echo "Error waiting for Sonar quality gate: ${err}"
                  currentBuild.result = 'UNSTABLE'
                }
              }
            } catch (err) {
              echo "ERROR running SonarQube analysis: ${err}"
              echo 'Common causes: SONAR_TOKEN missing/wrong, Sonar server misconfigured in Jenkins, Sonar server unreachable from agent.'
              currentBuild.result = 'UNSTABLE'
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
          } else {
            echo 'Skipping integration tests (no pom.xml found)'
          }
        }
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: '**/target/failsafe-reports/*.xml'
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
          if (env.PROMOTE_TO_PROD == 'true' || env.PROMOTE_TO_PROD == 'True') {
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