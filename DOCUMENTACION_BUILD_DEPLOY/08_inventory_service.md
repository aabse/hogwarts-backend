# `inventory-service`: `build.gradle` y `Dockerfile`

Archivos cubiertos:

- [`inventory-service/build.gradle`](../inventory-service/build.gradle)
- [`inventory-service/gradle.properties`](../inventory-service/gradle.properties)
- [`inventory-service/Dockerfile`](../inventory-service/Dockerfile)

**Función del servicio**: gestiona el inventario mágico — varas (`wands`), pociones (`potions`) y escobas (`brooms`), cada uno con su propio controller/service/repository, más una operación para asignar una vara a un wizard (`/api/inventory/wands`, `/api/inventory/potions`, `/api/inventory/brooms`, `POST /api/inventory/wands/{id}/assign`).

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

Es prácticamente idéntico al de [`wizard-service`](07_wizard_service.md): mismo Spring Boot 4.0.1, mismas dependencias base (`data-jpa`, `webmvc`, `validation`, Lombok, H2 en runtime, PostgreSQL + conector de Cloud SQL para el perfil `gcp`, mismo set de dependencias de test). Como los otros módulos:

- **Autocontenido a propósito**: declara las versiones de sus plugins y dependencias y su propio `repositories { mavenCentral() }`, aunque el root ya lo define. Así compila aunque Cloud Build reciba solo esta carpeta (build con Buildpacks). Por la misma razón no usa el catálogo `gradle/libs.versions.toml` del root.
- **Versión propia** en [`inventory-service/gradle.properties`](../inventory-service/gradle.properties) (`version=0.0.1-SNAPSHOT`): es el nombre del jar y el tag de su imagen, y se incrementa con versionado semántico independiente de los otros servicios (ver [`02_build_gradle_raiz.md`](02_build_gradle_raiz.md#versión-de-cada-servicio) y `DESPLIEGUE_GKE_GCP/03_artifact_registry_y_builds.md` §4).
- Hereda del [`build.gradle` raíz](02_build_gradle_raiz.md) el `group`, el toolchain de Java 17 y Checkstyle/Spotless/JaCoCo.

**Única diferencia relevante frente a `wizard-service`/`duel-service`**: **no incluye** `spring-retry` ni `spring-aspects`. Esto es coherente con que `inventory-service` no expone (por ahora) ninguna operación con manejo explícito de reintentos por bloqueo optimista — a diferencia del "ataque" de wizards o la creación de duelos, la asignación de varas no requiere ese mecanismo.

## `Dockerfile`

```dockerfile
# Dockerfile educativo — no usado por el flujo de despliegue documentado en
# DESPLIEGUE_GKE_GCP (que usa Cloud Build + buildpacks). Sirve para comparar
# manualmente los mismos pasos que un buildpack hace por vos.
#
# El build context debe ser la RAIZ del repo (no esta carpeta), porque este
# modulo es parte de un build multi-proyecto Gradle y necesita el wrapper y
# el settings.gradle de la raiz:
#
#   docker build -f inventory-service/Dockerfile -t inventory-service:local .
#   docker run -p 8080:8080 inventory-service:local

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
RUN ./gradlew :inventory-service:bootJar -x test -x checkstyleMain -x checkstyleTest -x spotlessCheck --no-daemon

# --- Etapa 2: imagen de runtime, liviana (solo JRE) ---
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /workspace/inventory-service/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Sigue el mismo patrón multi-stage que [`wizard-service/Dockerfile`](07_wizard_service.md#dockerfile) (explicado en detalle ahí). Solo cambian la tarea de Gradle (`:inventory-service:bootJar`) y la ruta del jar copiado (`inventory-service/build/libs/*.jar`). El build context sigue siendo la raíz del repo por la misma razón (build multi-módulo):

```bash
docker build -f inventory-service/Dockerfile -t inventory-service:local .
```

Como el de `wizard-service`, es **educativo**: el flujo de despliegue documentado (GKE + Buildpacks, ver [`README.md`](README.md) y `DESPLIEGUE_GKE_GCP/03_artifact_registry_y_builds.md`) construye la imagen directamente desde `inventory-service/` con `gcloud builds submit --pack`, sin usar este archivo. En los dos caminos, la imagen se publica con la versión de `inventory-service/gradle.properties` como tag (guía 03 §4.7).
