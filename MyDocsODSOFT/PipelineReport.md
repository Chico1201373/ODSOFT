# 🧠 Jenkinsfile Explained 

This Jenkinsfile describes a **CI/CD pipeline** for building, testing, analyzing, and deploying a Java app using **Maven**, **SonarQube**, and **Docker**.

---

## ⚙️ Overall Structure

The pipeline has multiple **stages** that run one after another.  
Each stage does something specific (like build, test, or deploy).  
It also uses some **environment variables** at the top for reuse later.

---

## 🧩 Environment Variables

```groovy
environment {
    DOCKER_IMAGE = "myapp:${env.BUILD_NUMBER}"
    SONAR_PROJECT_KEY = 'myapp-sonar'
}
```
DOCKER_IMAGE: The Docker image name with the current build number.
SONAR_PROJECT_KEY: Used for the SonarQube code analysis.

---
## 🏗️ Stages

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
Runs Maven to build the project and create a .jar file.
After it succeeds, the .jar file is saved as an artifact.

---
### SonarQube Analysis
```groovy
sh 'mvn clean verify sonar:sonar -Dsonar.projectKey=myapp -Dsonar.login=$SONAR_TOKEN'

```

Runs SonarQube to analyze the code quality (e.g., bugs, code smells).

---
### Unit Testing
```groovy
sh 'mvn test'

```

Runs unit tests.
After the tests:
    JUnit reports are collected.
    Jacoco coverage is checked.

---
### Mutation Testing
```groovy
sh 'mvn org.pitest:pitest-maven:mutationCoverage'
```

Runs mutation testing (checks how good your tests are by introducing fake bugs).

---
### Integration Tests
```groovy
sh 'mvn verify'
```

Runs integration tests.
Same as before, collects JUnit and Jacoco results.

---
### Build Docker Image
Only runs if a Dockerfile exists.
It logs in to Docker Hub and builds an image for the app:

```groovy
docker build -t ${DOCKER_IMAGE} .
```

Runs integration tests.
Same as before, collects JUnit and Jacoco results.

---
### Deploy
This stage depends on the branch:

    develop → runs the app directly (dev environment)
    staging → runs it in Docker (on port 8090)
    main → deploys to production (Docker, port 80)

Basically:

```groovy
# Develop 
java -jar target/myapp.jar

# Staging 
docker run -d -p 8090:8090 myapp:BUILD_NUMBER

# Production 
docker run -d --name myapp -p 80:8090 myapp:BUILD_NUMBER
```
---
## 🧾 Post Actions
After the pipeline finishes:

    If it fails → prints ❌
    If it succeeds → prints ✅
---
## 🚀 Summary

| 🧩 Stage | 🎯 Purpose |
|-----------|------------|
| Checkout | Get the code |
| Build & Package | Compile the app |
| SonarQube | Code quality check |
| Unit Testing | Run small tests |
| Mutation Testing | Check test strength |
| Integration Tests | Run bigger tests |
| Build Docker Image | Create Docker version of app |
| Deploy | Send app to correct environment |


---