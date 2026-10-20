# `duel-service`: `build.gradle` y `Dockerfile`

Archivos cubiertos:

- [`duel-service/build.gradle`](../duel-service/build.gradle)
- [`duel-service/gradle.properties`](../duel-service/gradle.properties)
- [`duel-service/Dockerfile`](../duel-service/Dockerfile)

**Función del servicio**: gestiona duelos entre wizards — creación de un duelo y consulta de su estado/resultado (`POST /api/duels`, `GET /api/duels/{id}`), con un modelo de dominio `Duel` + `DuelStatus`.

## `build.gradle`

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.1'
    id 'io.spring.dependency-management' version '1.1.7'
}

// Se repite aqui (aunque el root ya lo define) para que el modulo compile aislado:
// el build con buildpacks sube solo esta carpeta a Cloud Build.
repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-webmvc'
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    // Retry (optimistic locking)
    implementation 'org.springframework.retry:spring-retry:2.0.12'
    implementation 'org.springframework:spring-aspects'

    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    runtimeOnly 'com.h2database:h2'

    // Cloud SQL Java Connector (perfil "gcp")
    runtimeOnly 'org.postgresql:postgresql'
    implementation 'com.google.cloud.sql:postgres-socket-factory:1.28.1'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-data-jpa-test'
    testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

Es **idéntico** al de [`wizard-service`](07_wizard_service.md) dependencia por dependencia, incluyendo `spring-retry` + `spring-aspects` (coherente con que la creación/actualización de un duelo probablemente involucra bloqueo optimista sobre los wizards participantes, igual que la operación de ataque) y PostgreSQL + conector de Cloud SQL para el perfil `gcp`. Como los otros módulos:

- **Autocontenido a propósito**: declara las versiones de sus plugins y dependencias y su propio `repositories { mavenCentral() }`, aunque el root ya lo define. Así compila aunque Cloud Build reciba solo esta carpeta (build con Buildpacks). Por la misma razón no usa el catálogo `gradle/libs.versions.toml` del root.
- **Versión propia** en [`duel-service/gradle.properties`](../duel-service/gradle.properties) (`version=0.0.1-SNAPSHOT`): es el nombre del jar y el tag de su imagen, y se incrementa con versionado semántico independiente de los otros servicios (ver [`02_root_build_gradle.md`](02_root_build_gradle.md#versión-de-cada-servicio) y `GKE_GCP_DEPLOYMENT/03_artifact_registry_and_builds.md` §4).
- Hereda del [`build.gradle` raíz](02_root_build_gradle.md) el `group`, el toolchain de Java 17 y la configuración de Checkstyle/Spotless/JaCoCo.

## `Dockerfile`

```dockerfile
# Dockerfile educativo — no usado por el flujo de despliegue documentado en
# GKE_GCP_DEPLOYMENT (que usa Cloud Build + buildpacks). Sirve para comparar
# manualmente los mismos pasos que un buildpack hace por vos.
#
# El build context debe ser la RAIZ del repo (no esta carpeta), porque este
# modulo es parte de un build multi-proyecto Gradle y necesita el wrapper y
# el settings.gradle de la raiz:
#
#   docker build -f duel-service/Dockerfile -t duel-service:local .
#   docker run -p 8080:8080 duel-service:local

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
RUN ./gradlew :duel-service:bootJar -x test -x checkstyleMain -x checkstyleTest -x spotlessCheck --no-daemon

# --- Etapa 2: imagen de runtime, liviana (solo JRE) ---
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /workspace/duel-service/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Sigue el mismo patrón multi-stage que [`wizard-service/Dockerfile`](07_wizard_service.md#dockerfile) (explicado en detalle ahí), cambiando solo el módulo objetivo (`:duel-service:bootJar`) y la ruta del jar (`duel-service/build/libs/*.jar`):

```bash
docker build -f duel-service/Dockerfile -t duel-service:local .
```

Es **educativo**, como los otros dos. El flujo de despliegue documentado (GKE + Buildpacks, ver [`README.md`](README.md) y `GKE_GCP_DEPLOYMENT/03_artifact_registry_and_builds.md`) construye la imagen con `gcloud builds submit --pack` ejecutado desde `duel-service/`, usando como tag la versión de `duel-service/gradle.properties` (guía 03 §4.7).
