# `wizard-service`: `build.gradle` and `Dockerfile`

Files covered:

- [`wizard-service/build.gradle`](../wizard-service/build.gradle)
- [`wizard-service/gradle.properties`](../wizard-service/gradle.properties)
- [`wizard-service/Dockerfile`](../wizard-service/Dockerfile)

They are documented together because they are directly related: `build.gradle` defines how the module is compiled and packaged (the `bootJar` task), and the `Dockerfile` does nothing more than invoke that same task inside a container and copy the resulting jar into a runtime image.

**What the service does**: it manages wizards — wizard CRUD and an "attack" operation between them (`/api/wizards`, `/api/wizards/attack`). Together with `duel-service`, it is the only module that uses `spring-retry` to handle optimistic locking (probably because of concurrent writes on the same wizard during an attack/duel).

## `build.gradle`

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.1'
    id 'io.spring.dependency-management' version '1.1.7'
}

// Repeated here (even though the root defines it) so the module builds in isolation:
// the buildpacks build uploads only this folder to Cloud Build.
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

    // Cloud SQL Java Connector ("gcp" profile)
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

Key points:
- **Spring Boot 4.0.1** + **Spring Dependency Management 1.1.7** — unlike the root `build.gradle`, the Spring Boot plugin is applied here, because this module **is** a runnable application (it produces a bootable jar via the `bootJar`/`bootRun` tasks).
- **Self-contained on purpose**: it declares the versions of its plugins and dependencies and its own `repositories { mavenCentral() }`, even though the root already defines it. That way it compiles even when Cloud Build receives only this folder (Buildpacks build). For the same reason it does not use the root's `gradle/libs.versions.toml` catalog.
- **Its own version** in [`wizard-service/gradle.properties`](../wizard-service/gradle.properties) (`version=0.0.1-SNAPSHOT`): it is the jar name and its image tag, and it is incremented with semantic versioning independently of the other services (see [`02_root_build_gradle.md`](02_root_build_gradle.md#version-of-each-service) and `GKE_GCP_DEPLOYMENT/03_artifact_registry_and_builds.md` §4).
- It inherits from the `subprojects` block of the [root `build.gradle`](02_root_build_gradle.md) the `group` (`com.magic`), the Java 17 toolchain and Checkstyle/Spotless/JaCoCo (only in the build from the root, not in the isolated Buildpacks build).
- `spring-boot-starter-data-jpa` + `h2` (`runtimeOnly`): persistence with JPA on an H2 database (in this module, with no explicit datasource configuration in `application.properties`, so Spring Boot falls back to its default behavior).
- `postgresql` (`runtimeOnly`) + `postgres-socket-factory` (Cloud SQL Java connector): used with the `gcp` profile to connect to Cloud SQL instead of H2 (see `GKE_GCP_DEPLOYMENT/06_cloud_sql_database.md`).
- `spring-retry` + `spring-aspects`: retry support via AOP, typically used together with JPA's `@Version` to handle `OptimisticLockingFailureException` in concurrent operations (such as an attack between wizards).
- It defines no image packaging task (`bootBuildImage`, `jib`, etc.) — packaging into an image happens **outside** Gradle, through this module's `Dockerfile` or through Buildpacks (see [`README.md`](README.md)).

## `Dockerfile`

```dockerfile
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
```

It is a **multi-stage Dockerfile** (two `FROM`s):

1. **`build` stage** (`eclipse-temurin:17-jdk-jammy`, with the full JDK):
   - Copies into the container **everything needed for a multi-module build**: the wrapper (`gradlew`, `gradlew.bat`, `gradle/`), the root configuration files (`settings.gradle`, `build.gradle`), `config/` (Checkstyle) and **the three service folders** (`wizard-service`, `inventory-service`, `duel-service`) — even though only one will be compiled, Gradle needs to see the whole multi-module tree declared in `settings.gradle` to resolve the build correctly.
   - Runs `./gradlew :wizard-service:bootJar -x test -x checkstyleMain -x checkstyleTest -x spotlessCheck --no-daemon`: it produces the executable jar **explicitly skipping** tests, Checkstyle and Spotless (for an image that builds faster), assuming those checks were already run earlier with `./gradlew check` in another step of the flow (see [`README.md`](README.md)). `--no-daemon` avoids leaving a persistent Gradle process inside the ephemeral container.
2. **Final stage** (`eclipse-temurin:17-jre-jammy`, JRE only, no build tools):
   - Copies only the generated jar (`COPY --from=build .../build/libs/*.jar app.jar`) — the result is a much lighter final image that includes neither the JDK, nor Gradle, nor the source code of the other two services.
   - `EXPOSE 8080`: matches the port declared by the GKE `Deployment` manifest (`containerPort: 8080`) and the one Spring Boot listens on unless configured otherwise.
   - `ENTRYPOINT ["java", "-jar", "app.jar"]`: starts the jar directly.

**An important detail highlighted in the file's own comment**: the **build context must be the repo root**, not `wizard-service/` — it is invoked as `docker build -f wizard-service/Dockerfile -t wizard-service:local .` (note the final `.` pointing to the root). And the file describes itself as **"educational"**: it helps you understand/replicate by hand what a buildpack does, but it **is not** the mechanism used in the actual deployment flow documented in `GKE_GCP_DEPLOYMENT/` (which uses Cloud Build + Buildpacks, no Dockerfile). Both flows are compared in detail in [`README.md`](README.md).

To publish the image with its semantic version as the tag (instead of `wizard-service:local`), see `GKE_GCP_DEPLOYMENT/03_artifact_registry_and_builds.md` §4.7.

See also: [`08_inventory_service.md`](08_inventory_service.md) and [`09_duel_service.md`](09_duel_service.md), whose `Dockerfile`s follow this same pattern.
