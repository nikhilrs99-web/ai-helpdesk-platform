FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY common/pom.xml common/
COPY services/notification-service/pom.xml services/notification-service/
# Download dependencies for offline build (optional, skipped for brevity)
COPY common/src common/src
COPY services/notification-service/src services/notification-service/src
RUN mvn clean package -pl services/notification-service -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/services/notification-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
