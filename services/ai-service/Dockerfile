FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY common/pom.xml common/
COPY services/ai-service/pom.xml services/ai-service/
COPY services/ticket-service/pom.xml services/ticket-service/
COPY services/kb-service/pom.xml services/kb-service/
COPY services/notification-service/pom.xml services/notification-service/
COPY services/analytics-service/pom.xml services/analytics-service/
COPY services/api-gateway/pom.xml services/api-gateway/
COPY common/src common/src
COPY services/ai-service/src services/ai-service/src
RUN mvn clean package -pl services/ai-service -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/services/ai-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
