pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = "localhost:5000"
        APP_NAME = "library-service"
        IMAGE_TAG = "${env.BUILD_NUMBER}"  // unique tag per build
        SONAR_HOST = "http://sonarqube:9000"
        SONAR_TOKEN = credentials('sonar-token') // Jenkins credential
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timestamps()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Unit Test') {
            steps {
                sh 'mvn -B clean test jacoco:report'
                junit 'target/surefire-reports/*.xml'
            }
        }

        stage('Static Analysis') {
            steps {
                sh "mvn sonar:sonar -Dsonar.host.url=${SONAR_HOST} -Dsonar.login=${SONAR_TOKEN}"
            }
        }

        stage('Mutation Tests') {
            steps {
                sh 'mvn org.pitest:pitest-maven:mutationCoverage'
                publishHTML(target: [
                    reportDir: 'target/pit-reports',
                    reportFiles: 'index.html',
                    reportName: 'Mutation Tests'
                ])
            }
        }

        stage('Integration Tests') {
            steps {
                sh 'mvn -Pintegration-test verify'
                junit 'target/failsafe-reports/*.xml'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh "docker build -t ${DOCKER_REGISTRY}/${APP_NAME}:${IMAGE_TAG} ."
                sh "docker push ${DOCKER_REGISTRY}/${APP_NAME}:${IMAGE_TAG}"
            }
        }

        stage('Deploy to Dev') {
            steps {
                echo "Deploying to dev environment..."
                sh "IMAGE_TAG=${IMAGE_TAG} docker compose -f docker-compose.dev.yml up -d"
            }
        }

        stage('Promote to Staging') {
            steps {
                input message: 'Deploy to staging environment?'
                sh "IMAGE_TAG=${IMAGE_TAG} docker compose -f docker-compose.staging.yml up -d"
            }
        }

        stage('Promote to Production') {
            steps {
                input message: 'Deploy to production environment?'
                sh "IMAGE_TAG=${IMAGE_TAG} docker compose -f docker-compose.prod.yml up -d"
            }
        }
    }

    post {
        success {
            echo "✅ Build, test, and deployment succeeded!"
        }
        failure {
            echo "❌ Build failed — check logs."
        }
    }
}
