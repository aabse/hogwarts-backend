# `duel-service`: `build.gradle` and `Dockerfile`

Files covered:

- [`duel-service/build.gradle`](../duel-service/build.gradle)
- [`duel-service/gradle.properties`](../duel-service/gradle.properties)
- [`duel-service/Dockerfile`](../duel-service/Dockerfile)

**What the service does**: it manages duels between wizards — creating a duel and querying its status/result (`POST /api/duels`, `GET /api/duels/{id}`), with a `Duel` + `DuelStatus` domain model.

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

It is **identical** to [`wizard-service`](07_wizard_service.md)'s, dependency by dependency, including `spring-retry` + `spring-aspects` (consistent with the fact that creating/updating a duel probably involves optimistic locking on the participating wizards, just like the attack operation) and PostgreSQL + the Cloud SQL connector for the `gcp` profile. Like the other modules:

- **Self-contained on purpose**: it declares the versions of its plugins and dependencies and its own `repositories { mavenCentral() }`, even though the root already defines it. That way it compiles even when Cloud Build receives only this folder (Buildpacks build). For the same reason it does not use the root's `gradle/libs.versions.toml` catalog.
- **Its own version** in [`duel-service/gradle.properties`](../duel-service/gradle.properties) (`version=0.0.1-SNAPSHOT`): it is the jar name and its image tag, and it is incremented with semantic versioning independently of the other services (see [`02_root_build_gradle.md`](02_root_build_gradle.md#version-of-each-service) and `GKE_GCP_DEPLOYMENT/03_artifact_registry_and_builds.md` §4).
- It inherits from the [root `build.gradle`](02_root_build_gradle.md) the `group`, the Java 17 toolchain and the Checkstyle/Spotless/JaCoCo configuration.

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
#   docker build -f duel-service/Dockerfile -t duel-service:local .
#   docker run -p 8080:8080 duel-service:local

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
RUN ./gradlew :duel-service:bootJar -x test -x checkstyleMain -x checkstyleTest -x spotlessCheck --no-daemon

# --- Stage 2: lightweight runtime image (JRE only) ---
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /workspace/duel-service/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

It follows the same multi-stage pattern as [`wizard-service/Dockerfile`](07_wizard_service.md#dockerfile) (explained in detail there), changing only the target module (`:duel-service:bootJar`) and the jar path (`duel-service/build/libs/*.jar`):

```bash
docker build -f duel-service/Dockerfile -t duel-service:local .
```

It is **educational**, like the other two. The documented deployment flow (GKE + Buildpacks, see [`README.md`](README.md) and `GKE_GCP_DEPLOYMENT/03_artifact_registry_and_builds.md`) builds the image with `gcloud builds submit --pack` run from `duel-service/`, using the version from `duel-service/gradle.properties` as the tag (guide 03 §4.7).
