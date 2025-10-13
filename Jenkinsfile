pipeline {
    agent any

    environment {
        DOCKER_IMAGE = "myapp:${env.BUILD_NUMBER}"
        SONAR_PROJECT_KEY = 'myapp-sonar'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Package') {
            steps {
                sh 'mvn clean package'
            }
            post {
                success {
                    archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
                }
            }
        }

        stage('Static Code Analysis') {
            steps {
                // Use SONAR_TOKEN from Jenkins Credentials
                withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_AUTH_TOKEN')]) {
                    sh "mvn sonar:sonar -Dsonar.projectKey=${SONAR_PROJECT_KEY} -Dsonar.login=$SONAR_AUTH_TOKEN"
                }
            }
        }

        stage('Unit Testing') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                    jacoco(execPattern: '**/target/jacoco.exec', classPattern: '**/target/classes', sourcePattern: 'src/main/java')
                }
            }
        }

        stage('Mutation Testing') {
            steps {
                sh 'mvn org.pitest:pitest-maven:mutationCoverage'
            }
        }

        stage('Integration Testing') {
            steps {
                sh 'mvn verify -Pintegration-tests'
            }
            post {
                always {
                    junit '**/target/failsafe-reports/*.xml'
                }
            }
        }

        stage('Build Docker Image') {
            when {
                expression { fileExists('Dockerfile') }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-creds', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    sh """
                        echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin
                        docker build -t ${DOCKER_IMAGE} .
                    """
                }
            }
        }

        stage('Deploy to Local') {
            steps {
                sh 'nohup java -jar target/myapp.jar &'
            }
        }

        stage('Deploy to Docker') {
            when {
                expression { fileExists('Dockerfile') }
            }
            steps {
                sh "docker run -d -p 8080:8080 ${DOCKER_IMAGE}"
            }
        }
    }

    post {
        failure {
          echo "Pipeline unsuccessfully!"

        }
        success {
            echo "Pipeline completed successfully!"
        }
    }
}
