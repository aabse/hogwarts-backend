# Hogwarts Backend (Spring Boot, Multi-module)

Multi-module Gradle project containing three Spring Boot microservices:

- **`wizard-service`**: wizards (houses, levels, duel/attack operation in current phase)
- **`inventory-service`**: inventory (wands, potions, brooms)
- **`duel-service`**: duels (create + fetch duel outcomes)

This repo uses **Java 17**, Gradle Wrapper, and enforces formatting, static analysis, tests, and coverage via Gradle `check`.

## Requirements

- **JDK 17**
- **Gradle Wrapper** (use `./gradlew`, no global Gradle required)

On macOS you can confirm you have Java 17 installed:

```bash
/usr/libexec/java_home -V
```

## Quick start (build + tests)

Run the full quality gate (format check, checkstyle, tests, and coverage verification):

```bash
./gradlew check
```

If Gradle is picking a wrong JVM, force JDK 17:

```bash
JAVA_HOME="$(/usr/libexec/java_home -v 17)" ./gradlew check
```

Run tests for all modules (without the full `check` pipeline):

```bash
./gradlew testAll
```

## Run locally

Each microservice is a standalone Spring Boot app. You can run each one with `bootRun`.

This repo does **not** pin `server.port` in `application.properties`, so to run multiple services at once you should pass ports explicitly.

### Run `wizard-service`

```bash
./gradlew :wizard-service:bootRun --args="--server.port=8080"
```

### Run `inventory-service`

```bash
./gradlew :inventory-service:bootRun --args="--server.port=8081"
```

### Run `duel-service`

```bash
./gradlew :duel-service:bootRun --args="--server.port=8082"
```

### API endpoints (high-level)

Controllers and base paths:

- **Wizard**: `/api/wizards` (includes `/attack`)
- **Inventory**: `/api/inventory/wands`, `/api/inventory/potions`, `/api/inventory/brooms`
- **Duel**: `/api/duels`

For full requests/examples, import the Postman collection:

- [`Hogwarts_API.postman_collection.json`](Hogwarts_API.postman_collection.json)

It includes environment variables that assume:

- `wizardBaseUrl`: `http://localhost:8080`
- `inventoryBaseUrl`: `http://localhost:8081`
- `duelBaseUrl`: `http://localhost:8082`

## Quality gates

### Formatting (Spotless)

Check formatting:

```bash
./gradlew spotlessCheck
```

Auto-format:

```bash
./gradlew spotlessApply
```

### Checkstyle

```bash
./gradlew checkstyleMain checkstyleTest
```

### Coverage (JaCoCo)

This repo enforces an **aggregate JaCoCo coverage gate** (overall) as part of `check`.

Generate the aggregate report:

```bash
./gradlew jacocoRootReport
```

Open the HTML report here:

- `build/reports/jacoco/jacocoRootReport/html/index.html`

Verify the gate explicitly:

```bash
./gradlew jacocoRootCoverageVerification
```

## GCP profile (`gcp`)

With `--spring.profiles.active=gcp` each service connects to Cloud SQL (PostgreSQL) through the Cloud SQL Java Connector. Credentials are **not** stored in the repo; provide them as environment variables:

| Variable | Required | Description |
|---|---|---|
| `DB_PASSWORD` | yes | Database user password |
| `INSTANCE_CONNECTION_NAME` | yes | `project:region:instance` |
| `DB_USER` | no | Defaults to `<service>_user` |

## Documentation

- [`DESPLIEGUE_GKE_GCP/`](DESPLIEGUE_GKE_GCP/README.md): step-by-step deployment guides for GKE/GCP (Artifact Registry, Cloud SQL, Secret Manager, Identity Platform, observability).
- [`DOCUMENTACION_BUILD_DEPLOY/`](DOCUMENTACION_BUILD_DEPLOY/README.md): build setup (Gradle wrapper, Checkstyle, per-service modules, container images).
- [`DEPENDENCIAS.md`](DEPENDENCIAS.md), [`DTOs.md`](DTOs.md), [`SPECIFICATIONS_SPRING.md`](SPECIFICATIONS_SPRING.md), [`ROADMAP_AVANZADO.md`](ROADMAP_AVANZADO.md)
