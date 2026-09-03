FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY common/pom.xml common/
COPY services/analytics-service/pom.xml services/analytics-service/
# Download dependencies for offline build (optional, skipped for brevity)
COPY common/src common/src
COPY services/analytics-service/src services/analytics-service/src
RUN mvn clean package -pl services/analytics-service -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/services/analytics-service/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
