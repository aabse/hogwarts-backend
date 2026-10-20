# `settings.gradle`

File covered: [`settings.gradle`](../settings.gradle)

```groovy
rootProject.name = 'hogwarts-backend'

include 'wizard-service'
include 'inventory-service'
include 'duel-service'
```

## What it does

`settings.gradle` is the **entry point** of a Gradle build: it is evaluated before any `build.gradle`, and it is where the topology of the multi-module project is defined.

- `rootProject.name = 'hogwarts-backend'`: the logical name of the root project (used, for example, in the artifact name if the root were packaged, and in Gradle messages).
- `include 'wizard-service' / 'inventory-service' / 'duel-service'`: declares the three subprojects. Each `include` tells Gradle to look for a folder with that name at the root and to expect its own `build.gradle` there (see [`07_wizard_service.md`](07_wizard_service.md), [`08_inventory_service.md`](08_inventory_service.md), [`09_duel_service.md`](09_duel_service.md)).

Without this file, `./gradlew :wizard-service:bootJar` (or any task using project notation, `:module:task`) would not exist — Gradle would not know that `wizard-service` is a module of the build.

## Relationship with the rest of the build

- The `subprojects { ... }` block of [`build.gradle`](02_root_build_gradle.md) (Checkstyle, Spotless, JaCoCo, Java 17) is applied exactly to the modules listed here.
- The `:module:task` notation that appears in:
  - the [`wizard-service` `Dockerfile`](07_wizard_service.md) (`./gradlew :wizard-service:bootJar -x test ...`),
  - the [root `README.md`](../README.md) to run each service locally (`./gradlew :inventory-service:bootRun --args="--server.port=8081"`),

  only works because `settings.gradle` included those modules.
- If a fourth microservice were added in the future, the first step would be to add its `include` here.

## Role in the build/deploy pipeline

It is a small but critical file: it defines which modules exist and therefore which container images are "buildable" (one per module). Both the **Dockerfile** flow (which needs to copy `settings.gradle` into the image because it compiles from the root, see [`README.md`](README.md)) and the **Buildpacks** flow (which runs from inside each subfolder, see `GKE_GCP_DEPLOYMENT/03_artifact_registry_and_builds.md`) depend on this topology to know what to compile in isolation.
