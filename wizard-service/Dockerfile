# Dockerfile educativo — no usado por el flujo de despliegue documentado en
# GKE_GCP_DEPLOYMENT (que usa Cloud Build + buildpacks). Sirve para comparar
# manualmente los mismos pasos que un buildpack hace por vos.
#
# El build context debe ser la RAIZ del repo (no esta carpeta), porque este
# modulo es parte de un build multi-proyecto Gradle y necesita el wrapper y
# el settings.gradle de la raiz:
#
#   docker build -f wizard-service/Dockerfile -t wizard-service:local .
#   docker run -p 8080:8080 wizard-service:local

# --- Etapa 1: compilar el jar con Gradle ---
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

# --- Etapa 2: imagen de runtime, liviana (solo JRE) ---
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /workspace/wizard-service/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
