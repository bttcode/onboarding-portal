# Build context: repository root (the build needs ../api/openapi.yaml)
#   docker build -f backend/Dockerfile .

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY api/openapi.yaml api/openapi.yaml
COPY backend/pom.xml backend/pom.xml
RUN mvn -B -f backend/pom.xml dependency:go-offline
COPY backend/src backend/src
RUN mvn -B -f backend/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/backend/target/onboarding-backend.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
