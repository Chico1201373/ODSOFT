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

        stage('SonarQube Analysis') {
    steps {
        withSonarQubeEnv('SonarQubeServer') {
            sh 'mvn clean verify sonar:sonar -Dsonar.projectKey=myapp -Dsonar.login=$SONAR_TOKEN'
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

        stage('Integration Tests') {
    steps {
        sh 'mvn verify'
    }
    post {
        always {
            junit '**/target/surefire-reports/*.xml'
            
            jacoco execPattern: '**/target/jacoco.exec', 
                   classPattern: '**/target/classes', 
                   sourcePattern: '**/src/main/java'
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
                sh "docker run -d -p 8090:8090 ${DOCKER_IMAGE}"
            }
        }
    }

    post {
        failure {
          echo "Pipeline completed unsuccessfully!"

        }
        success {
            echo "Pipeline completed successfully!"
        }
    }
}
