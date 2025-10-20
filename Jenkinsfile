pipeline {
    agent any

    environment {
        BRANCH_NAME_SANITIZED = "${env.GIT_BRANCH ?: env.BRANCH_NAME}".replaceAll('/', '-')
        DOCKER_IMAGE = "myapp:${BRANCH_NAME_SANITIZED}"
        SONAR_PROJECT_KEY = 'myapp-sonar'
    }


    stages {

        stage('Checkout') {
            steps {
                checkout scm
                script {
                    // For multibranch pipelines, Jenkins automatically sets env.BRANCH_NAME
                    echo "Current branch: ${env.BRANCH_NAME}"
                    echo "Docker image name: ${DOCKER_IMAGE}"
                }
            }
        }

        stage('Build & Package') {
            steps {
                sh 'mvn clean package -DskipTests'
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
                    recordCoverage(
                        tools: [[parser: 'JACOCO', pattern: 'target/site/jacoco/jacoco.xml']],
                        id: 'jacoco-unit',
                        name: 'JaCoCo Unit Coverage',
                        sourceCodeRetention: 'LAST_BUILD',
                        sourceDirectories: [[path: 'src/main/java'], [path: 'target/generated-sources/annotations']],
                        qualityGates: [
                            [metric: 'LINE',   baseline: 'PROJECT', threshold: 80.0, criticality: 'NOTE'],
                            [metric: 'BRANCH', baseline: 'PROJECT', threshold: 70.0, criticality: 'NOTE']
                        ]
                    )
                }
            }
        }

        stage('Mutation Testing') {
            steps {
                sh 'mvn org.pitest:pitest-maven:mutationCoverage'
            }
            post {
                always {
                    recordCoverage(
                        tools: [[parser: 'PIT', pattern: 'target/pit-reports/latest/mutations.xml']],
                        id: 'pit',
                        name: 'PIT Mutation Coverage',
                        sourceCodeRetention: 'LAST_BUILD',
                        sourceDirectories: [[path: 'src/main/java'], [path: 'target/generated-sources/annotations']],
                        qualityGates: [[metric: 'MUTATION', baseline: 'PROJECT', threshold: 70.0, criticality: 'NOTE']]
                    )
                }
            }
        }

        stage('Integration Tests') {
            steps {
                sh 'mvn jacoco:prepare-agent-integration failsafe:integration-test failsafe:verify'
            }
            post {
                always {
                    junit '**/target/failsafe-reports/*.xml'
                    recordCoverage(
                        tools: [[parser: 'JACOCO', pattern: 'target/site/jacoco-it/jacoco.xml']],
                        id: 'jacoco-it',
                        name: 'JaCoCo IT Coverage',
                        sourceCodeRetention: 'LAST_BUILD',
                        sourceDirectories: [[path: 'src/main/java'], [path: 'target/generated-sources/annotations']],
                        qualityGates: [
                            [metric: 'LINE',   baseline: 'PROJECT', threshold: 80.0, criticality: 'NOTE'],
                            [metric: 'BRANCH', baseline: 'PROJECT', threshold: 70.0, criticality: 'NOTE']
                        ]
                    )
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
                        docker push ${DOCKER_IMAGE}
                    """
                }
            }
        }


        stage('Deploy to Development') {
            when {
                branch 'develop'
            }
            steps {
                echo "🚀 Deploying to DEVELOPMENT (local JAR)..."
                sh '''
                    echo "Stopping existing process..."
                    pkill -f "psoft-g1-0.0.1-SNAPSHOT.jar" || true
                    echo "Starting application..."
                    nohup java -jar target/psoft-g1-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
                '''
            }
        }

        stage('Deploy to Staging') {
            when {
                branch 'staging'
            }
            steps {
                echo "🚀 Deploying to STAGING (Docker)..."
                sh '''
                    docker stop myapp || true
                    docker rm myapp || true
                    docker run -d --name myapp -p 8090:8090 ${DOCKER_IMAGE}
                '''
            }
        }

        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            steps {
                echo "🚀 Deploying to PRODUCTION (Docker)..."
                sh '''
                    docker stop myapp || true
                    docker rm myapp || true
                    docker run -d --name myapp -p 80:8090 ${DOCKER_IMAGE}
                '''
            }
        }
    }


    post {
        failure {
            echo "❌ Pipeline completed unsuccessfully!"
        }
        success {
            echo "✅ Pipeline completed successfully!"
        }
    }
}
