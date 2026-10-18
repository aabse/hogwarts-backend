# `wizard-service`: `build.gradle` y `Dockerfile`

Archivos cubiertos:

- [`wizard-service/build.gradle`](../wizard-service/build.gradle)
- [`wizard-service/gradle.properties`](../wizard-service/gradle.properties)
- [`wizard-service/Dockerfile`](../wizard-service/Dockerfile)

Se documentan juntos porque están directamente relacionados: `build.gradle` define cómo se compila y empaqueta el módulo (tarea `bootJar`), y el `Dockerfile` no hace otra cosa que invocar esa misma tarea dentro de un contenedor y copiar el jar resultante a una imagen de runtime.

**Función del servicio**: gestiona a los magos (wizards) — CRUD de wizards y una operación de "ataque" entre ellos (`/api/wizards`, `/api/wizards/attack`). Es, junto con `duel-service`, el único módulo que usa `spring-retry` para manejar bloqueo optimista (probablemente por escrituras concurrentes sobre el mismo wizard durante un ataque/duelo).

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

Puntos clave:
- **Spring Boot 4.0.1** + **Spring Dependency Management 1.1.7** — a diferencia del `build.gradle` raíz, aquí sí se aplica el plugin de Spring Boot, porque este módulo **es** una aplicación ejecutable (produce un jar arrancable vía la tarea `bootJar`/`bootRun`).
- **Autocontenido a propósito**: declara las versiones de sus plugins y dependencias y su propio `repositories { mavenCentral() }`, aunque el root ya lo define. Así compila aunque Cloud Build reciba solo esta carpeta (build con Buildpacks). Por la misma razón no usa el catálogo `gradle/libs.versions.toml` del root.
- **Versión propia** en [`wizard-service/gradle.properties`](../wizard-service/gradle.properties) (`version=0.0.1-SNAPSHOT`): es el nombre del jar y el tag de su imagen, y se incrementa con versionado semántico independiente de los otros servicios (ver [`02_build_gradle_raiz.md`](02_build_gradle_raiz.md#versión-de-cada-servicio) y `DESPLIEGUE_GKE_GCP/03_artifact_registry_y_builds.md` §4).
- Hereda del bloque `subprojects` del [`build.gradle` raíz](02_build_gradle_raiz.md) el `group` (`com.magic`), el toolchain de Java 17 y Checkstyle/Spotless/JaCoCo (solo en el build desde la raíz, no en el aislado de Buildpacks).
- `spring-boot-starter-data-jpa` + `h2` (`runtimeOnly`): persistencia con JPA sobre una base H2 (en este módulo, sin configuración explícita de datasource en `application.properties`, por lo que Spring Boot cae al comportamiento por defecto).
- `postgresql` (`runtimeOnly`) + `postgres-socket-factory` (conector Java de Cloud SQL): se usan con el perfil `gcp`, para conectarse a Cloud SQL en lugar de H2 (ver `DESPLIEGUE_GKE_GCP/06_base_de_datos_cloud_sql.md`).
- `spring-retry` + `spring-aspects`: soporte de reintentos vía AOP, usado típicamente junto con `@Version` de JPA para manejar `OptimisticLockingFailureException` en operaciones concurrentes (como el ataque entre wizards).
- No define ninguna tarea de empaquetado de imagen (`bootBuildImage`, `jib`, etc.) — el empaquetado en imagen se hace **fuera** de Gradle, vía el `Dockerfile` de este mismo módulo o vía Buildpacks (ver [`README.md`](README.md)).

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
```

Es un **Dockerfile multi-stage** (dos `FROM`):

1. **Etapa `build`** (`eclipse-temurin:17-jdk-jammy`, con JDK completo):
   - Copia al contenedor **todo lo necesario para un build multi-módulo**: el wrapper (`gradlew`, `gradlew.bat`, `gradle/`), los archivos de configuración raíz (`settings.gradle`, `build.gradle`), `config/` (Checkstyle) y **las tres carpetas de servicios** (`wizard-service`, `inventory-service`, `duel-service`) — aunque solo se vaya a compilar una, Gradle necesita ver todo el árbol multi-módulo declarado en `settings.gradle` para resolver el build correctamente.
   - Ejecuta `./gradlew :wizard-service:bootJar -x test -x checkstyleMain -x checkstyleTest -x spotlessCheck --no-daemon`: genera el jar ejecutable **saltando explícitamente** tests, Checkstyle y Spotless (para una imagen más rápida de construir), asumiendo que esas verificaciones ya se corrieron antes con `./gradlew check` en otro paso del flujo (ver [`README.md`](README.md)). `--no-daemon` evita dejar un proceso Gradle persistente dentro del contenedor efímero.
2. **Etapa final** (`eclipse-temurin:17-jre-jammy`, solo JRE, sin herramientas de compilación):
   - Copia únicamente el jar generado (`COPY --from=build .../build/libs/*.jar app.jar`) — el resultado es una imagen final mucho más liviana que no incluye el JDK, Gradle, ni el código fuente de los otros dos servicios.
   - `EXPOSE 8080`: coincide con el puerto que el manifiesto `Deployment` de GKE declara (`containerPort: 8080`) y con el que Spring Boot escucha si no se configura otro.
   - `ENTRYPOINT ["java", "-jar", "app.jar"]`: arranque directo del jar.

**Detalle importante remarcado en el propio comentario del archivo**: el **build context debe ser la raíz del repo**, no `wizard-service/` — se invoca como `docker build -f wizard-service/Dockerfile -t wizard-service:local .` (nótese el `.` final apuntando a la raíz). Y el propio archivo se autodescribe como **"educativo"**: sirve para entender/replicar manualmente lo que hace un buildpack, pero **no es** el mecanismo usado en el flujo de despliegue real documentado en `DESPLIEGUE_GKE_GCP/` (que usa Cloud Build + Buildpacks, sin Dockerfile). Ambos flujos se comparan en detalle en [`README.md`](README.md).

Para publicar la imagen con su versión semántica como tag (en vez de `wizard-service:local`), ver `DESPLIEGUE_GKE_GCP/03_artifact_registry_y_builds.md` §4.7.

Ver también: [`08_inventory_service.md`](08_inventory_service.md) y [`09_duel_service.md`](09_duel_service.md), cuyos `Dockerfile` siguen este mismo patrón.
