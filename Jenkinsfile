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
                    // jacoco(execPattern: '**/target/jacoco.exec', classPattern: '**/target/classes', sourcePattern: 'src/main/java')
                    // publishHTML([allowMissing: false, alwaysLinkToLastBuild: true, icon: '', keepAll: true, reportDir: 'target/site/jacoco', reportFiles: 'index.html', reportName: 'Coverage – Unit', reportTitles: '', useWrapperFileDirectly: true])
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
            // post {
            //     always {
            //         recordCoverage(
            //             tools: [[parser: 'PIT', pattern: 'target/pit-reports/latest/mutations.xml']],
            //             id: 'pit',
            //             name: 'PIT Mutation Coverage',
            //             sourceCodeRetention: 'LAST_BUILD',
            //             qualityGates: [
            //             // Exemple : échouer si le score de mutation < 70%
            //             [threshold: 70.0, metric: 'MUTATION', baseline: 'PROJECT', unstable: false]
            //             ]
            //         )
            //         archiveArtifacts artifacts: 'target/pit-reports/**', fingerprint: true, onlyIfSuccessful: false
            //     }
            // }
        }

        stage('Integration Tests') {
            steps {
                sh 'mvn jacoco:prepare-agent-integration failsafe:integration-test failsafe:verify'
            }
            post {
                always {
                    junit '**/target/failsafe-reports/*.xml'
                    // jacoco(execPattern: '**/target/jacoco-it.exec', classPattern: '**/target/classes', sourcePattern: 'src/main/java')
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
                    // publishHTML([allowMissing: false, alwaysLinkToLastBuild: true, icon: '', keepAll: true, reportDir: 'target/site/jacoco-it', reportFiles: 'index.html', reportName: 'Coverage – IT', reportTitles: '', useWrapperFileDirectly: true])
                }
            }
        }

         stage('Coverage Report') {
            steps {
                echo "📊 Generating unified coverage report..."
                recordCoverage(
                    tools: [jacoco(name: 'JaCoCo Coverage')],
                    sourceCodeRetention: 'EVERY_BUILD',
                    adapters: [
                        jacocoAdapter('**/target/site/jacoco/jacoco.xml'),
                        jacocoAdapter('**/target/site/jacoco-it/jacoco.xml'),
                        jacocoAdapter('*/modules//jacoco.xml') // for multi-module builds
                    ],
                    failNoReports: true,
                    qualityGates: [
                        [threshold: 80, metric: 'LINE', unstable: true],
                        [threshold: 70, metric: 'BRANCH', unstable: true]
                    ]
                )
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

        stage('Deploy') {
            when {
                anyOf {
                    branch 'develop'
                    branch 'staging'
                    branch 'main'
                }
            }
            steps {
                script {
                    if (env.BRANCH_NAME == 'develop') {
                        echo "🚀 Deploying to DEVELOPMENT environment..."
                        sh 'nohup java -jar target/myapp.jar &'
                    } else if (env.BRANCH_NAME == 'staging') {
                        echo "🚀 Deploying to STAGING environment (Docker)..."
                        sh "docker run -d -p 8090:8090 ${DOCKER_IMAGE}"
                    } else if (env.BRANCH_NAME == 'main') {
                        echo "🚀 Deploying to PRODUCTION environment (Docker)..."
                        sh """
                            docker stop myapp || true
                            docker rm myapp || true
                            docker run -d --name myapp -p 80:8090 ${DOCKER_IMAGE}
                        """
                    }
                }
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
