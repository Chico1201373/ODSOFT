% 🧠 Jenkinsfile Explained

This document explains the project's `Jenkinsfile` (declarative pipeline). It maps the pipeline's environment variables, stages, conditional behavior, and post actions to the actual pipeline steps used in CI for this Java/Maven application.

The `Jenkinsfile` is designed for multibranch pipelines and includes steps for build, unit and integration testing, mutation testing (PIT), SonarQube analysis, code coverage recording, conditional Docker image creation and push (for `staging`/`main`), and environment-specific deployment.

## Quick contract

- Inputs: Git repository checked out by Jenkins, credentials for SonarQube (`SONAR_TOKEN`) and Docker Hub (`docker-creds`).
- Outputs: built JAR artifact(s), test reports (JUnit), coverage reports (JaCoCo, PIT), Docker image pushed for `staging`/`main` and deployments for `develop`/`main` branches.
- Success criteria: Maven build completes, tests run and reports are archived, SonarQube analysis is executed, Docker image is built and pushed when on `staging` or `main`, and deployment runs for `develop` or `main` as configured.

## Environment variables used

The pipeline declares a small set of environment variables at the top of the `Jenkinsfile`:

- `BRANCH_NAME_SANITIZED` — a branch name with `/` replaced by `-`. Used to create valid Docker tags.
- `DOCKER_IMAGE` — set to `myapp:${BRANCH_NAME_SANITIZED}` and used as a convenience tag in the pipeline.
- `SONAR_PROJECT_KEY` — the SonarQube project key used when invoking the Sonar scanner.

These variables are evaluated by Jenkins at runtime. The `BRANCH_NAME_SANITIZED` value is computed like:

```groovy
BRANCH_NAME_SANITIZED = "${env.GIT_BRANCH ?: env.BRANCH_NAME}".replaceAll('/', '-')
```

This ensures the branch name is safe for Docker tags and other artifact names.

## Stages (detailed)

1) Checkout

    - Purpose: obtain repository contents.
    - Implementation: `checkout scm` (multibranch-aware). The pipeline echoes the branch and image name for debugging.

2) Build & Package

    - Purpose: compile and package the application into a JAR using Maven.
    - Implementation: `sh 'mvn clean package -DskipTests'` — builds the application and skips tests to speed up the packaging stage.
    - Post: on success the pipeline archives any `target/*.jar` artifact(s) with fingerprints.

3) SonarQube Analysis

    - Purpose: run static analysis and quality metrics in SonarQube.
    - Implementation: wraps the Maven Sonar goal in `withSonarQubeEnv('SonarQubeServer')` and runs:

      `mvn clean verify sonar:sonar -Dsonar.projectKey=myapp -Dsonar.login=$SONAR_TOKEN`

    - Notes: requires Jenkins to have a SonarQube installation configured under the name `SonarQubeServer` and a secret `SONAR_TOKEN` credential in the job.

4) Unit Testing

    - Purpose: execute unit tests and record test/coverage results.
    - Implementation: `sh 'mvn test'`.
    - Post steps (always):
      - Publish JUnit XML reports from `target/surefire-reports/*.xml`.
      - Record JaCoCo coverage using `recordCoverage` with the JaCoCo XML at `target/site/jacoco/jacoco.xml` and quality gates for LINE/BRANCH thresholds.

5) Mutation Testing (PIT)

    - Purpose: measure unit-test strength via mutation testing.
    - Implementation: `sh 'mvn org.pitest:pitest-maven:mutationCoverage'`.
    - Post steps: `recordCoverage` is invoked with the PIT report `target/pit-reports/latest/mutations.xml` and a mutation threshold.

6) Integration Tests

    - Purpose: run integration tests with Maven Failsafe and collect coverage.
    - Implementation: `sh 'mvn jacoco:prepare-agent-integration failsafe:integration-test failsafe:verify'` — prepares JaCoCo for integration tests then runs Failsafe.
    - Post steps: publish `target/failsafe-reports/*.xml` and record JaCoCo IT coverage from `target/site/jacoco-it/jacoco.xml`.

7) Build Docker Image (conditional)

    - When it runs: only if a `Dockerfile` exists in the repo AND the branch is `staging` or `main`.
    - What it does:
      - Uses Jenkins credentials `docker-creds` (username + password) to log in to Docker Hub.
      - Builds an image: `docker build -t "$DOCKER_USER/myapp:${BRANCH_NAME_SANITIZED}" .`
      - Pushes it: `docker push "$DOCKER_USER/myapp:${BRANCH_NAME_SANITIZED}"`

    - Notes: the pipeline assumes Docker is installed on the Jenkins agent and that the `docker-creds` credential exists.

8) Deploy to Production (main branch)

    - When: only runs on the `main` branch.
    - What it does:
      - Logs into Docker Hub using `docker-creds`.
      - Pulls the `main` image: `docker pull "$DOCKER_USER/myapp:main"`.
      - Stops & removes an existing `myapp` container if present and starts a new one mapped to port 80 (host) → 8090 (container):

         `docker run -d --name myapp -p 80:8090 "$DOCKER_USER/myapp:main"`

    - Notes: this is a simple Docker-based deployment. In production you may prefer orchestration (K8s, ECS) and health checks.

9) Deploy to Development (develop branch)

    - When: only runs on the `develop` branch.
    - What it does:
      - Kills any running process matching the current JAR name.
      - Starts the built JAR in background (`nohup java -jar target/psoft-g1-0.0.1-SNAPSHOT.jar > app.log 2>&1 &`).

    - Notes: this provides a quick dev runtime on the Jenkins agent. For reproducible dev environments you may prefer Docker or a dedicated dev host.

## Post actions

The pipeline defines post conditions for the whole pipeline:

- `failure` — prints a failure message.
- `success` — prints a success message.

These messages are simple indicators. You can extend them to send Slack/Teams notifications or create GitHub status checks.

## Edge cases and recommendations

- Credentials: pipeline depends on `SONAR_TOKEN` (SonarQube) and `docker-creds` (Docker Hub). Ensure these are configured in Jenkins credentials store and are available to the job.
- Docker availability: the `Build Docker Image` and production `Deploy` steps require the Jenkins agent to have Docker CLI and permission to run it.
- Resource cleanup: running the JAR on the Jenkins agent (`develop` deploy) can leave orphaned processes; the pipeline attempts to pkill the previous one but consider more robust lifecycle management.
- Image tagging: the pipeline tags Docker images by sanitized branch names. Consider including the build number or Git commit short SHA for immutable tags.
- SonarQube gating: the pipeline runs Sonar analysis but doesn't explicitly wait for quality gate results. If you want to block on Sonar quality gates, add a `waitForQualityGate()` step (requires Sonar plugin configured in Jenkins).

## Summary table

| Stage | Purpose | Runs on | Key artifacts |
|---|---:|---|---|
| Checkout | Get code | any | repo snapshot |
| Build & Package | Build JAR | any | target/*.jar |
| SonarQube | Static analysis | any | Sonar results |
| Unit Testing | Unit tests + JaCoCo | any | surefire XML, jacoco.xml |
| Mutation Testing | PIT mutation report | any | target/pit-reports/latest/mutations.xml |
| Integration Tests | Failsafe + JaCoCo IT | any | failsafe XML, jacoco-it.xml |
| Build Docker Image | Build & push image | staging, main (if Dockerfile exists) | Docker image in registry |
| Deploy to Production | Run Docker container | main | running container (myapp) |
| Deploy to Development | Run JAR on agent | develop | background Java process |

## Completion

This document mirrors the `Jenkinsfile` behavior and adds notes and recommendations to make the pipeline more robust and production-ready. Update the Jenkins credentials and Sonar configuration in Jenkins to match your environment.
