pipeline {
  agent any

  options {
    buildDiscarder(logRotator(daysToKeepStr: '30', numToKeepStr: '50'))
    timestamps()
    ansiColor('xterm')
    disableConcurrentBuilds()
  }

  environment {
    APP_NAME    = 'books-api'
    // Docker registry settings (override with Jenkins credentials/vars)
    REGISTRY    = credentials('docker-registry-url') ?: 'docker.io'
    DOCKER_CREDENTIALS = 'docker-creds'

    // Default image repo base (override via pipeline parameter or env var)
    IMAGE_BASE = "chico0706/${APP_NAME}"
  }

  parameters {
    booleanParam(name: 'PROMOTE_TO_PROD', defaultValue: false, description: 'When true and on staging, promote the built image to production tag')
    string(name: 'IMAGE_TAG', defaultValue: '', description: 'Optional override for image tag (defaults to branch or build number)')
    string(name: 'SONAR_SERVER_NAME', defaultValue: 'MySonarServer', description: 'Name of the SonarQube server configured in Jenkins (Manage Jenkins → Configure System → SonarQube servers)')
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

    stage('Static Analysis - SonarQube') {
      when { expression { fileExists('pom.xml') } }
      steps {
        // Use the SonarQube server name provided as a parameter so it's configurable per-job
        withSonarQubeEnv(params.SONAR_SERVER_NAME) {
          // bind token just for the sonar call
          withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
            script {
              echo "SONAR_SERVER=${params.SONAR_SERVER_NAME}"
              echo "SONAR_HOST_URL=${env.SONAR_HOST_URL}"
              try {
                // Pass JaCoCo XML report path to Sonar if available
                def coverageArg = ''
                if (env.JACOCO_PATH) {
                  coverageArg = "-Dsonar.coverage.jacoco.xmlReportPaths=${env.JACOCO_PATH}"
                  echo "Using JaCoCo report: ${env.JACOCO_PATH}"
                } else {
                  echo 'No JaCoCo report detected; continuing without sonar.coverage.jacoco.xmlReportPaths'
                }

                sh "mvn sonar:sonar -Dsonar.login=$SONAR_TOKEN -Dsonar.host.url=$SONAR_HOST_URL ${coverageArg}"

                // Wait for SonarQube Quality Gate result (requires SonarQube plugin in Jenkins)
                timeout(time: 5, unit: 'MINUTES') {
                  script {
                    def qg = waitForQualityGate(abortPipeline: false)
                    if (qg == null) {
                      echo 'No quality gate result available yet.'
                    } else {
                      echo "Sonar Quality Gate status: ${qg.status}"
                      if (qg.status != 'OK') {
                        unstable "Quality gate not OK: ${qg.status}"
                      }
                    }
                  }
                }

              } catch (err) {
                // Don't hard fail the whole pipeline for Sonar misconfigurations — surface useful debug info instead
                echo "ERROR running SonarQube analysis: ${err}"
                echo 'Common causes: SONAR_TOKEN missing/wrong, Sonar server misconfigured in Jenkins, Sonar server unreachable from agent.'
                echo 'Check Manage Jenkins → Configure System → SonarQube servers, and confirm SONAR_TOKEN credential id.'
                currentBuild.result = 'UNSTABLE'
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