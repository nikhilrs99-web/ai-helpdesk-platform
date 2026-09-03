FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY common/pom.xml common/
COPY services/ticket-service/pom.xml services/ticket-service/
# Download dependencies for offline build (optional, skipped for brevity)
COPY common/src common/src
COPY services/ticket-service/src services/ticket-service/src
RUN mvn clean package -pl services/ticket-service -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/services/ticket-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
