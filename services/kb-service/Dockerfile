FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY common/pom.xml common/
COPY services/kb-service/pom.xml services/kb-service/
COPY services/ticket-service/pom.xml services/ticket-service/
COPY services/notification-service/pom.xml services/notification-service/
COPY services/ai-service/pom.xml services/ai-service/
COPY services/analytics-service/pom.xml services/analytics-service/
COPY services/api-gateway/pom.xml services/api-gateway/
# Download dependencies for offline build (optional, skipped for brevity)
COPY common/src common/src
COPY services/kb-service/src services/kb-service/src
RUN mvn clean package -pl services/kb-service -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Run as an unprivileged, non-login user - the base image runs as root by default, and this
# process never needs to be root (it doesn't bind privileged ports or touch host files).
RUN addgroup -S app && adduser -S -G app app
COPY --from=builder --chown=app:app /app/services/kb-service/target/*.jar app.jar
USER app
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
