pipeline {

    triggers {
        githubPush()
    }

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

 //  Build & Push Docker image only for staging or main
        stage('Build Docker Image') {
            when {
                expression { fileExists('Dockerfile') }

                anyOf {
                    branch 'staging'
                    branch 'main'
                }
            }
            steps {
                echo " Building Docker image for ${BRANCH_NAME_SANITIZED}"
                withCredentials([usernamePassword(credentialsId: 'docker-creds', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    sh '''
                        echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                        docker build -t "$DOCKER_USER/myapp:${BRANCH_NAME_SANITIZED}" .
                        docker push "$DOCKER_USER/myapp:${BRANCH_NAME_SANITIZED}"
                    '''
                }
            }
        }

        //  Only deploy when on main branch (production)
        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            steps {
                echo "Deploying to PRODUCTION (Docker)..."
                withCredentials([usernamePassword(credentialsId: 'docker-creds', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    sh '''
                        echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                        docker pull "$DOCKER_USER/myapp:main"
                        docker stop myapp || true
                        docker rm myapp || true
                        docker run -d --name myapp -p 8081:8081 "$DOCKER_USER/myapp:main"                    
                        '''
                }
            }
        }

        // Local run for develop
        stage('Deploy to Development') {
            when {
                branch 'develop'
            }
            steps {
                echo "🚀 Running local JAR for DEVELOPMENT..."
                sh '''
                    pkill -f "psoft-g1-0.0.1-SNAPSHOT.jar" || true
                    nohup java -jar target/psoft-g1-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
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
