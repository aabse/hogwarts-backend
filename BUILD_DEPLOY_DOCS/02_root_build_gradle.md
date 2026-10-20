# `build.gradle` (root)

Files covered:

- [`build.gradle`](../build.gradle)
- [`gradle/libs.versions.toml`](../gradle/libs.versions.toml)

It is Gradle's shared configuration file: it defines what applies to the root project and, through the `subprojects { ... }` block, what is applied automatically to the three microservices (`wizard-service`, `inventory-service`, `duel-service`). It does not declare the Spring Boot plugin — that lives in each service's `build.gradle` (see [`07_wizard_service.md`](07_wizard_service.md), [`08_inventory_service.md`](08_inventory_service.md), [`09_duel_service.md`](09_duel_service.md)), because the root project itself is not a runnable application, only the "container" for the common configuration.

## Plugins and root project metadata

```groovy
plugins {
    id 'java'
    id 'jacoco'
    alias(libs.plugins.spotless) apply false
}

group = 'com.magic'
version = '0.0.1-SNAPSHOT'
description = 'hogwarts'
```

- `java` and `jacoco` are also applied to the root project.
- `spotless` is declared with `apply false`: its version comes from the version catalog (see the next section), but it is only **activated** inside `subprojects`, not on the root.
- `group` (`com.magic`) is always inherited by the three services.
- `version` (`0.0.1-SNAPSHOT`) is only the **default value**: each service declares its own version in its `gradle.properties` (see the section [Version of each service](#version-of-each-service)).

## Version catalog: `gradle/libs.versions.toml`

```toml
[versions]
spotless = "8.1.0"
jacoco = "0.8.12"
checkstyle = "12.3.1"

[plugins]
spotless = { id = "com.diffplug.spotless", version.ref = "spotless" }
```

Gradle loads this file automatically (because it is in `gradle/` with that name) and exposes it in the scripts as `libs`. It centralizes in a single place the versions of the **root build tools**:

| Tool | Catalog entry | Where it is used in `build.gradle` |
|---|---|---|
| Spotless | `libs.plugins.spotless` | `plugins` block |
| JaCoCo | `libs.versions.jacoco.get()` | `jacoco { toolVersion = ... }`, on the root and in `subprojects` |
| Checkstyle | `libs.versions.checkstyle.get()` | `checkstyle { toolVersion = ... }`, in `subprojects` |

To update one of these tools, change only its line in `[versions]`.

**Why Spring Boot and the services' dependencies are not in the catalog**: the Buildpacks build (see [`README.md`](README.md) §4.a) runs from each service's folder and Cloud Build uploads **only that folder**. `gradle/libs.versions.toml` does not exist there, so a service `build.gradle` using `libs.…` would not compile. That is why each service declares its own versions (Spring Boot, `spring-retry`, Cloud SQL connector) and its own `repositories` block. The cost is that the Spring Boot version is repeated in the three service `build.gradle` files: when updating it, you have to change it in all three.

## Version of each service

Each service is deployed and versioned separately, so its version lives in a `gradle.properties` inside its folder:

```properties
# wizard-service/gradle.properties
# Service's own version (used for the jar name and the image tags).
version=0.0.1-SNAPSHOT
```

Gradle reads that file when it creates the subproject. So that the root does not overwrite it, the `subprojects` block only assigns the root version to services that do not have their own:

```groovy
subprojects {
    group = rootProject.group
    // Each service defines its version in its gradle.properties; otherwise it inherits the root's.
    if (version.toString() == 'unspecified') {
        version = rootProject.version
    }
    ...
}
```

That version names the jar (`wizard-service-0.0.1-SNAPSHOT.jar`) and is also the container image tag. The rules for incrementing it (semantic versioning `MAJOR.MINOR.PATCH`) and the release flow are in [`../GKE_GCP_DEPLOYMENT/03_artifact_registry_and_builds.md`](../GKE_GCP_DEPLOYMENT/03_artifact_registry_and_builds.md) §4.

Since `gradle.properties` is inside the service folder, it also works in the isolated Buildpacks build.

To see a service's resolved version:

```bash
./gradlew -q :wizard-service:properties | grep '^version:'
```

## Java toolchain

```groovy
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
```

It pins **Java 17** both on the root and (repeated) inside `subprojects`. Gradle uses this toolchain to compile and run, regardless of the JDK installed by whoever runs the build (as long as Gradle can locate or download a JDK 17).

## The `subprojects { ... }` block

It is applied automatically to `wizard-service`, `inventory-service` and `duel-service` (the modules declared in [`settings.gradle`](03_settings_gradle.md)):

- Applies the `java`, `checkstyle`, `com.diffplug.spotless` and `jacoco` plugins.
- Assigns `group` and, if the service does not have its own, `version` (see [Version of each service](#version-of-each-service)).
- Repeats the Java 17 toolchain and the `mavenCentral()` repository.
- **Checkstyle**:
  ```groovy
  checkstyle {
      toolVersion = libs.versions.checkstyle.get()
      configFile = rootProject.file('config/checkstyle/checkstyle.xml')
      configProperties = [
              'checkstyle.suppressions.file': rootProject.file('config/checkstyle/suppressions.xml').absolutePath
      ]
  }
  ```
  See the full details of the rules in [`04_checkstyle.md`](04_checkstyle.md). Checkstyle reports are forced to XML and HTML (`tasks.withType(Checkstyle).configureEach { reports { xml.required = true; html.required = true } }`).
- **Spotless** (automatic Java formatting):
  ```groovy
  spotless {
      java {
          googleJavaFormat()
          formatAnnotations()
          trimTrailingWhitespace()
          endWithNewline()
      }
  }
  ```
  It uses Google Java Format. `tasks.named('check') { dependsOn 'spotlessCheck' }` makes incorrect formatting **break** `./gradlew check`.
- **JaCoCo per module**: `toolVersion = libs.versions.jacoco.get()` (0.8.12); `test` is `finalizedBy 'jacocoTestReport'` (the coverage report is generated automatically after every `test`), with XML and HTML reports forced.

## Custom root-level tasks

```groovy
tasks.register('testAll') {
    dependsOn(subprojects.collect { it.tasks.matching { t -> t.name == 'test' } })
}
```
Runs the tests of the three modules with a single command (`./gradlew testAll`), without running the rest of the quality gate (spotless/checkstyle/coverage).

```groovy
def coverageExclusions = [
        '**/*Application.*',
        '**/*DTO.*',
        '**/common/**',
        '**/domain/model/**',
        '**/infrastructure/persistence/**',
        '**/package-info.*'
]

tasks.register('jacocoRootReport', JacocoReport) { ... }
tasks.register('jacocoRootCoverageVerification', JacocoCoverageVerification) { ... }
```

- `jacocoRootReport`: aggregates the coverage `.exec` files of the three modules into a single report (`build/reports/jacoco/jacocoRootReport/{jacocoRootReport.xml,html}`), excluding from the metric the `*Application` classes, DTOs, `common` code, domain models, the persistence layer and `package-info`.
- `jacocoRootCoverageVerification`: applies a **minimum line coverage gate of 90%** (`counter = 'LINE'`, `value = 'COVEREDRATIO'`, `minimum = 0.90`) on that aggregated report.
- Finally:
  ```groovy
  tasks.named('check') {
      dependsOn 'jacocoRootCoverageVerification'
  }
  ```
  This chains everything together: `./gradlew check` on the root triggers compilation + tests of the three modules + `spotlessCheck` + Checkstyle + the 90% coverage gate. It is the command that validates that the project is ready to build/package before generating any deployment image.

## Role in the build/deploy pipeline

This is the file Gradle reads first (together with `settings.gradle` and the `gradle/libs.versions.toml` catalog) to know which quality rules and which tool versions apply to the **whole** monorepo. The services' `Dockerfile`s compile from the root, so they use this configuration, although they skip the quality steps (`-x test -x checkstyleMain -x spotlessCheck`) to speed up the image. The Buildpacks build, on the other hand, compiles each service in isolation and does **not** use this file. In both cases, what guarantees that the code meets these rules is `./gradlew check`, run beforehand (locally or in CI) before building the image. See the full flow in [`README.md`](README.md).
