FROM openjdk:17-jdk
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests clean package

FROM openjdk:17-jdk
ARG JAR_FILE=target/library-service-*.jar
COPY --from=build /app/${JAR_FILE} /app/library-service.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/library-service.jar"]