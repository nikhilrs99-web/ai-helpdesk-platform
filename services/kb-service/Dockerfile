FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY common/pom.xml common/
COPY services/kb-service/pom.xml services/kb-service/
# Download dependencies for offline build (optional, skipped for brevity)
COPY common/src common/src
COPY services/kb-service/src services/kb-service/src
RUN mvn clean package -pl services/kb-service -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/services/kb-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
