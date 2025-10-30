#  Jenkinsfile  

---

##  Overall Structure

The pipeline has multiple **stages** that run one after another.  
Each stage does something specific (like build, test, or deploy).  
It also uses some **environment variables** at the top for reuse later.

---

##  Environment Variables

```groovy
environment {
    DOCKER_IMAGE = "myapp:${env.BUILD_NUMBER}"
    SONAR_PROJECT_KEY = 'myapp-sonar'
}
```
DOCKER_IMAGE: The Docker image name with the current build number.
SONAR_PROJECT_KEY: Used for the SonarQube code analysis.

---
##  Stages

### Checkout

```groovy
checkout scm
```

Pulls the project code from the repository so Jenkins can work on it.

---
### Build & Package


```groovy
sh 'mvn clean package'

```
---
## Jenkinsfile 


### What the pipeline does (high level)

- Checks out the code from GitHub.
- Builds the Java project with Maven.
- Runs several types of tests (unit, mutation, integration).
- Runs SonarQube analysis for code quality.
- Optionally builds and pushes a Docker image for certain branches.
- Deploys the app depending on the branch (develop, staging, main).

### Important environment variables

- `DOCKER_IMAGE` — name used for the Docker image (it uses the branch name).
- `SONAR_PROJECT_KEY` — a key used by SonarQube when scanning the project.

These are defined at the top of the Jenkinsfile so the stages can reuse them.

### Main stages 

- Checkout: gets the source code from the repository.
- Build & Package: runs `mvn clean package -DskipTests` to compile and create the JAR. The artifact is stored by Jenkins.
- SonarQube Analysis: runs Sonar scanning to detect code smells and issues.
- Unit Testing: runs `mvn test`. JUnit results and JaCoCo coverage are collected.
- Mutation Testing: optional step that runs PIT to check how strong the tests are.
- Integration Tests: runs integration tests using the Failsafe plugin and collects reports.
- Build Docker Image: when on `staging` or `main` (and if a Dockerfile exists), builds and pushes an image to Docker Hub.
- Deploy to Development / Production: depending on branch, the pipeline either starts the JAR locally (development) or runs a Docker container (production).

### How branches affect the pipeline

- `develop` → pipeline may run the app locally with the packaged JAR.
- `staging` → pipeline builds and pushes a Docker image for testing on staging.
- `main` → pipeline builds, pushes and deploys the image to production (as defined in the file).

---