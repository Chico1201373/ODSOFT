pipeline {
  agent any
  environment {
    APP_NAME = "books-api"
    IMAGE_NAME = "odsoft/books-api"
    TAG = "${env.GIT_COMMIT ?: 'local'}"
    SONAR_HOST = "http://sonarqube:9000"
  }
  options {
    timestamps()
    buildDiscarder(logRotator(daysToKeepStr: '14'))
  }
  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Build & Compile') {
      steps {
        // Maven example
        sh "mvn -B -DskipTests clean package"
      }
      post { success { archiveArtifacts artifacts: 'target/*.jar', fingerprint: true } }
    }

    stage('Static Analysis') {
      steps {
        // run Sonar scanner
        withEnv(["SONAR_HOST_URL=${SONAR_HOST}"]) {
          sh "mvn sonar:sonar -Dsonar.host.url=${SONAR_HOST} -Dsonar.login=${SONAR_TOKEN}"
        }
      }
    }

    stage('Unit Tests') {
      steps {
        sh "mvn -B -DskipITs=false -DskipITs=false test"
      }
      post {
        always {
          junit 'target/surefire-reports/*.xml'
          jacoco execPattern: 'target/jacoco.exec', classPattern: 'target/classes', sourcePattern: 'src/main/java'
        }
      }
    }

    stage('Mutation Tests (PITest)') {
      steps {
        sh "mvn -DskipTests -DskipITs org.pitest:pitest-maven:mutationCoverage"
      }
      post {
        always {
          // publish PIT HTML artifact (target/pit-reports)
          archiveArtifacts artifacts: 'target/pit-reports/**', fingerprint: true
        }
      }
    }

    stage('Integration Tests') {
      steps {
        // Failsafe runs integration tests (mvn verify)
        sh "mvn -B -DskipTests=false -DskipITs=false verify"
      }
      post {
        always {
          junit 'target/failsafe-reports/*.xml'
        }
      }
    }

    stage('Build Docker Image') {
      steps {
        sh "docker build -t ${IMAGE_NAME}:${TAG} ."
      }
    }

    stage('Push Image') {
      when { expression { env.BRANCH_NAME == 'main' } }
      steps {
        withCredentials([usernamePassword(credentialsId: 'docker-creds', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
          sh "echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin"
          sh "docker push ${IMAGE_NAME}:${TAG}"
        }
      }
    }

    stage('Deploy to Dev') {
      when { branch 'develop' }
      steps {
        sh """
          # Exemplo: atualizar compose dev
          export IMAGE_TAG=${TAG}
          docker-compose -f docker-compose.dev.yml pull ${IMAGE_NAME} || true
          docker-compose -f docker-compose.dev.yml up -d --build
        """
      }
    }

    stage('Functional/System Tests') {
      steps {
        // exemplo: Karate or Postman Newman or k6
        sh "mvn -DskipTests=false -Dkarate.env=dev test -Dkarate.options='--tags @smoke'"
      }
    }

    stage('Promote to Staging/Prod') {
      when { branch 'main' }
      input message: "Promote to staging?"
      steps {
        sh "docker-compose -f docker-compose.staging.yml up -d --build"
      }
    }
  }

  post {
    always {
      archiveArtifacts artifacts: 'target/*.jar, target/*.war', allowEmptyArchive: true
    }
    success {
      echo "Pipeline succeeded"
    }
    failure {
      mail to: 'team@example.com', subject: "Build failed: ${env.JOB_NAME} ${env.BUILD_NUMBER}", body: "See Jenkins"
    }
  }
}
