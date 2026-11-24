FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY backend/pom.xml .
COPY backend/src src
RUN mvn -B -Dmaven.test.skip=true package
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S learnflow && adduser -S learnflow -G learnflow
COPY --from=build /build/target/learnflow-backend-0.1.0-SNAPSHOT.jar app.jar
USER learnflow
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
