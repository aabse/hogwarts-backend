# Educational Dockerfile — not used by the deployment flow documented in
# GKE_GCP_DEPLOYMENT (which uses Cloud Build + buildpacks). It lets you compare,
# step by step, what a buildpack does for you.
#
# The build context must be the repo ROOT (not this folder), because this
# module is part of a Gradle multi-project build and needs the root wrapper
# and settings.gradle:
#
#   docker build -f wizard-service/Dockerfile -t wizard-service:local .
#   docker run -p 8080:8080 wizard-service:local

# --- Stage 1: build the jar with Gradle ---
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace

COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle gradle
COPY config config
COPY wizard-service wizard-service
COPY inventory-service inventory-service
COPY duel-service duel-service

RUN chmod +x gradlew
RUN ./gradlew :wizard-service:bootJar -x test -x checkstyleMain -x checkstyleTest -x spotlessCheck --no-daemon

# --- Stage 2: lightweight runtime image (JRE only) ---
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /workspace/wizard-service/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
