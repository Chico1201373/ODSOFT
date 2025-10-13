pipeline {
  agent any

  options {
    buildDiscarder(logRotator(daysToKeepStr: '30', numToKeepStr: '50'))
    timestamps()
    disableConcurrentBuilds()
  }

  environment {
    // Application
    APP_NAME = 'books-api'

    // Maven/Java tools (ensure Jenkins installs these tools)
    MAVEN_HOME = tool name: 'maven-3.9.6', type: 'maven'
    JAVA_HOME = tool name: 'jdk17', type: 'jdk'

    // Sonar
    SONAR_TOKEN = credentials('SONAR_TOKEN')
    SONAR_SERVER = 'MySonarServer'
    SONAR_PROJECT_KEY = 'ODSOFT'

    // Docker registry credentials id
    DOCKER_CREDENTIALS = 'docker-creds'
  REGISTRY = 'docker.io'
  ARTIFACT_GLOB = 'target/*.jar'
  // Compose files for environments (change if your files are named differently)
  DEV_COMPOSE_FILE = 'docker-compose.dev.yml'
  STAGING_COMPOSE_FILE = 'docker-compose.staging.yml'
  PROD_COMPOSE_FILE = 'docker-compose.prod.yml'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Build - Unit Tests') {
      steps {
        sh '${MAVEN_HOME}/bin/mvn -B -V -DskipTests=false clean test'
      }
      post {
        always {
          junit '**/target/surefire-reports/TEST-*.xml'
          archiveArtifacts artifacts: '${ARTIFACT_GLOB}', allowEmptyArchive: true
        }
      }
    }

    stage('JaCoCo Coverage') {
      steps {
        sh '${MAVEN_HOME}/bin/mvn jacoco:prepare-agent test jacoco:report || true'
      }
      post {
        always {
          publishHTML([allowMissing: true, alwaysLinkToLastBuild: true, keepAll: true, reportDir: 'target/site/jacoco', reportFiles: 'index.html', reportName: 'JaCoCo Coverage'])
          archiveArtifacts artifacts: 'target/site/jacoco/jacoco.xml', allowEmptyArchive: true
        }
      }
    }

    stage('Mutation tests (PIT)') {
      steps {
        // run PIT mutation tests; requires PIT plugin configured in pom
        sh '${MAVEN_HOME}/bin/mvn org.pitest:pitest-maven:mutationCoverage || true'
      }
      post {
        always {
          archiveArtifacts artifacts: 'target/pit-reports/**', allowEmptyArchive: true
        }
      }
    }

    stage('Static Analysis - SonarQube') {
      steps {
        withSonarQubeEnv(SONAR_SERVER) {
          sh "${MAVEN_HOME}/bin/mvn sonar:sonar -Dsonar.projectKey=${SONAR_PROJECT_KEY} -Dsonar.login=${SONAR_TOKEN} -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml || true"
        }
      }
    }

    stage('Wait For Quality Gate') {
      steps {
        // Only works if Sonar webhook is configured to notify Jenkins
        script {
          try {
            timeout(time: 5, unit: 'MINUTES') {
              def qg = waitForQualityGate(abortPipeline: false)
              if (qg && qg.status != 'OK') {
                unstable "SonarQube quality gate: ${qg.status}"
              }
            }
          } catch (Exception e) {
            echo "Quality gate wait failed: ${e}. Continuing but mark UNSTABLE"
            currentBuild.result = 'UNSTABLE'
          }
        }
      }
    }

    stage('Integration Tests') {
      steps {
        sh '${MAVEN_HOME}/bin/mvn verify -DskipUnitTests=true'
      }
      post {
        always {
          junit '**/target/failsafe-reports/*.xml'
        }
      }
    }

    stage('Build Artifact') {
      steps {
        sh '${MAVEN_HOME}/bin/mvn -B -V -DskipTests=true package'
        archiveArtifacts artifacts: '${ARTIFACT_GLOB}', allowEmptyArchive: false
      }
    }

    stage('Build Docker Image') {
      steps {
        script {
          def imageTag = "${REGISTRY}/${env.BRANCH_NAME ?: 'local'}/${APP_NAME}:${env.BUILD_NUMBER}"
          sh "docker build -t ${imageTag} ."
          env.IMAGE_TAG = imageTag
        }
      }
    }

    stage('Push Image') {
      when { anyOf { branch 'staging'; branch 'main' } }
      steps {
        withCredentials([usernamePassword(credentialsId: DOCKER_CREDENTIALS, usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
          sh 'echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin ${REGISTRY}'
          sh 'docker push ${IMAGE_TAG}'
        }
      }
    }

    stage('Deploy to Dev (local)') {
      when { branch 'develop' }
      steps {
        sh 'docker-compose -f ${DEV_COMPOSE_FILE} up -d --build'
      }
    }

    stage('Deploy to Staging') {
      when { branch 'staging' }
      steps {
        script {
          echo 'Deploying to Staging via local docker-compose'
          sh "docker pull ${IMAGE_TAG} || true"
          sh "docker tag ${IMAGE_TAG} ${APP_NAME}:staging || true"
          sh "docker-compose -f ${STAGING_COMPOSE_FILE} up -d --build || true"
        }
      }
    }

    stage('Deploy to Production') {
      when { branch 'main' }
      steps {
        input message: 'Approve production deploy', ok: 'Deploy'
        script {
          echo 'Deploying to Production via local docker-compose'
          sh "docker pull ${IMAGE_TAG} || true"
          sh "docker tag ${IMAGE_TAG} ${APP_NAME}:prod || true"
          sh "docker-compose -f ${PROD_COMPOSE_FILE} up -d --build || true"
        }
      }
    }
  }

  post {
    success {
      echo 'Pipeline succeeded.'
    }
    unstable {
      echo 'Pipeline finished unstable. Check logs.'
    }
    failure {
      echo 'Pipeline failed.'
    }
  }
}
