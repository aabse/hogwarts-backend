# ADVANCED_ROADMAP — Spring Boot (advanced level) + Microservices + GCP (GKE)

This document proposes a backlog/roadmap **aimed at deep learning** of Spring and at a realistic evolution towards **domain-based microservices** deployed on **GCP**.  
The idea is that each epic forces you to deal with "real" topics: consistency, resilience, serious testing, observability, security, CI/CD, infrastructure.

---

## Principles and goals

- **Microservices**:
  - `wizard-service`: management of wizards, levels and houses.
  - `duel-service`: battle/duel logic (**this is where the retry with `@Retryable` lives**).
  - `inventory-service`: inventory management (wands, potions and brooms).
  - `config-server`: centralized configuration.
  - `api-gateway`: single entry point + security (JWT).
- **API-first**: clear contracts, versioning, consistent errors.
- **Reliability**: timeouts, sensible retries, idempotency, fault tolerance.
- **Observability by default**: structured logs + traces + metrics.
- **Security from the start**: authn/authz, secrets, least privilege.
- **Incremental evolution**: first modularize the monolith, then extract services.

---

## Phase 0 — Project baseline (minimum professional quality)

### 0.1 Standards and documentation
- [ ] **README** with how to run locally, endpoints, and the domain structure.
- [ ] **Commit convention**, branching, and versioning.
- [ ] **OpenAPI** for the endpoints (Swagger UI) and request/response examples.

**Definition of Done (DoD)**  
- The project runs locally with 1 command (or 2 at most) and is documented.
- There is documentation of the API and typical errors (400/404/409).

**Learning**: Spring Web, validation, API design, DX.

### 0.2 Serious testing (not just "context loads")
- [ ] Unit tests for services (business rules).
- [ ] WebMvcTest for controllers + validation.
- [ ] Integration with a real DB via **Testcontainers (Postgres)** (avoid H2 vs prod drift).
- [ ] Minimum coverage per module and quality criteria (not as a number, but by critical paths).

**DoD**  
- CI fails if critical tests fail.
- There are tests for the "happy path" and "edge cases".

**Learning**: Spring Test, Testcontainers, test slicing.

---

## Phase 1 — Prior modularization (pre-microservices)

Goal: prepare the code to be split painlessly.

- [ ] Reorganize packages by domain (for example):
  - `com.magic.hogwarts.wizard.*`
  - `com.magic.hogwarts.inventory.*`
  - `com.magic.hogwarts.duel.*`
- [ ] Define **DTOs** per layer (request/response), avoiding exposing JPA entities.
- [ ] Unify the error response (code, message, timestamp, path, traceId).
- [ ] Add pagination/filters to listings (Spring Data).

**DoD**
- There are no circular references between domains.
- Controllers do not return JPA entities directly.

**Learning**: domain-based architecture, layer separation, contracts.

---

## Phase 2 — `wizard-service` (microservice 1)

### 2.1 Extracting the service
- [ ] Turn the project into a **Gradle multi-module** build (or separate repos) and extract `wizard-service`.
- [ ] Define boundaries: the service owns `Wizard` and its DB.
- [ ] Create complete endpoints:
  - `POST /api/wizards`
  - `GET /api/wizards` (paginated + filters)
  - `GET /api/wizards/{id}`
  - `PATCH /api/wizards/{id}` (partial)
  - `DELETE /api/wizards/{id}` (if applicable)
- [ ] Enrich the model with rules (e.g. unique name per house, level limits, etc.).

**DoD**
- A `wizard-service` runs on its own and passes its test suite.
- Stable, versioned OpenAPI contract (v1).

**Learning**: modularization, DDD-lite, API design.

### 2.2 "Production" persistence
- [ ] Migrations with **Flyway** or **Liquibase**.
- [ ] Constraints and indexes (uniqueness, not-null, search indexes).
- [ ] Separate the "read model" from the "write model" when useful (optimized projection/DTO).

**DoD**
- The DB can be recreated from scratch with migrations only.
- The main queries are indexed and justified.

**Learning**: JPA/Hibernate, real SQL, migrations.

### 2.3 Concurrency, idempotency and consistency
- [ ] Keep **optimistic locking** (`@Version`) and **retry** with backoff.
- [ ] Introduce an **idempotency key** on critical write operations (if applicable).
- [ ] Validate invariants under concurrency (tests with multiple threads / scenarios).

**DoD**
- A conflict can be simulated and the system responds with 409 or retries correctly.

**Learning**: concurrency, consistency patterns.

### 2.4 Internal resilience (inside the service)
- [ ] Consistent timeouts on external calls (if any).
- [ ] Circuit breaker/bulkhead for dependencies (if you add integrations).

**Learning**: resilience (sensible retries, failures).

---

## Phase 3 — `inventory-service` (microservice 2)

### 3.1 Extraction and inventory model
- [ ] Extract `inventory-service` as the owner of:
  - `Wand`
  - `Potion`
  - `Broom`
- [ ] Advanced rules: stock, availability, assignment, change auditing.
- [ ] Endpoints:
  - `POST /api/inventory/wands`
  - `GET /api/inventory/wands` (paginated + filters)
  - `POST /api/inventory/potions`
  - `GET /api/inventory/potions` (paginated + filters)
  - `POST /api/inventory/brooms`
  - `GET /api/inventory/brooms` (paginated + filters)
  - `POST /api/inventory/wands/{wandId}/assign` (assign to a wizard)

**Architectural decision (key learning)**  
Is the wizard↔wand relationship resolved with:
- **(A) References by ID** (each service stores the other's ID) + validation via API/events, or
- **(B) Event-driven** (inventory-service emits "wand-assigned" and wizard-service materializes a read model)?

**DoD**
- The service operates independently, with migrations and tests.
- A versioned API contract.

**Learning**: boundaries, modeling, consistency between services.

---

## Phase 4 — `duel-service` (microservice 3)

### 4.1 Duel orchestration
- [ ] Extract `duel-service` as the owner of `Duel` (history, status, timestamps, participants).
- [ ] Endpoint:
  - `POST /api/duels` (creates a duel: attackerId, defenderId)
  - `GET /api/duels/{id}` (status + result)
- [ ] Decide the outcome on the server (by level, random, rules, etc.).
- [ ] Move/centralize here the **retry** logic (`@Retryable`) applied to the duel's effects.
- [ ] Auditing: store the event/result, allow "replay"/debugging.

**DoD**
- Each duel produces a persistent, reproducible record (same input → same result, if defined that way).

**Learning**: orchestration, auditing, flow design.

### 4.2 Cross-service consistency (advanced patterns)
- [ ] Implement a **Saga (orchestration)** if the duel affects several services.
- [ ] Implement a **Transactional Outbox** to publish events reliably.

**DoD**
- No "double updates" or lost events in case of crashes.

**Learning**: outbox, sagas, eventual consistency.

---

## Phase 5 — Communication between microservices

### 5.1 Synchronous (REST)
- [ ] Default timeouts, limited retries, circuit breaker.
- [ ] API versioning and backward compatibility.
- [ ] Contract testing (consumer-driven) between services.

### 5.2 Asynchronous (events)
- [ ] Define an event catalog:
  - `wizard-created`
  - `wizard-leveled-up`
  - `inventory-wand-created`
  - `inventory-wand-assigned`
  - `inventory-potion-created`
  - `inventory-broom-created`
  - `duel-finished`
- [ ] Design events with:
  - `eventId`, `eventType`, `occurredAt`, `schemaVersion`, `payload`
  - consumer idempotency (deduplication).

**DoD**
- Idempotent consumers + controlled event replay.

**Learning**: event-driven, schemas, distributed idempotency.

---

## Phase 6 — `api-gateway` (Edge)

- [ ] Add an `api-gateway` (e.g. Spring Cloud Gateway) for:
  - per-service routing
  - rate limiting
  - centralized authn/authz (JWT)
  - correlation-id and common headers
- [ ] Version the external routes (`/api/v1/...`) and map them to internal services.

**DoD**
- The client talks to the gateway; the services can be private.

**Learning**: edge patterns, centralized security.

---

## Phase 7 — Security (advanced)

- [ ] OAuth2/OIDC with JWT:
  - roles/authorities (e.g. `wizard:read`, `wizard:write`, `duel:execute`)
  - validation in the gateway and/or in each service
- [ ] Secret management:
  - local: `.env` / local vault (not committed)
  - cloud: GCP Secret Manager
- [ ] Abuse protection:
  - rate limiting in the gateway
  - payload limits, timeouts, headers.

**DoD**
- Protected routes, role-based permissions, and basic security tests.

**Learning**: Spring Security, OIDC, hardening.

---

## Phase 8 — Observability (real operations)

- [ ] Structured logging (JSON) + `traceId`/`correlationId`.
- [ ] Distributed tracing with **OpenTelemetry** (propagate the context between services).
- [ ] Metrics with **Micrometer** (latency, errors, saturation).
- [ ] Health: liveness/readiness, DB/broker indicators.

**DoD**
- You can follow a request end-to-end (gateway → service → service) with a trace.

**Learning**: o11y, traces, metrics, debugging.

---

## Phase 9 — Local platform + CI/CD

### 9.1 Containerization
- [ ] A `Dockerfile` per service (multi-stage, non-root user).
- [ ] `docker-compose` for dev:
  - Postgres per service (or one with separate schemas at first)
  - (later) a broker/event bus

### 9.2 CI/CD (GitHub Actions or Cloud Build)
- [ ] Pipeline: build + tests + analysis (SAST) + SBOM + image scanning.
- [ ] Image versioning and promotion across environments (dev/stg/prod).

**DoD**
- Reproducible build, signed/scanned images, and automated deployment.

**Learning**: DevOps, supply chain, automation.

---

## Phase 10 — GCP hands-on (GKE)

### 10.1 GKE
- [ ] Create a GCP project and enable the APIs:
  - Artifact Registry
  - GKE
  - Cloud SQL
  - Secret Manager
  - Pub/Sub
  - Cloud Logging/Monitoring
- [ ] Artifact Registry:
  - Docker repo for the images
- [ ] Choose the packaging:
  - Helm charts per service, or
  - Kustomize overlays per environment
- [ ] Deploy to GKE (one per service):
  - namespaces per environment/team
  - HPA
  - PodDisruptionBudget
  - requests/limits
- [ ] Identity:
  - Workload Identity (avoid keys)
- [ ] Ingress + routing (or a gateway in the cluster).
- [ ] Cloud SQL (Postgres) per service (recommended) + Cloud SQL Auth Proxy as a sidecar.
- [ ] Secret Manager for credentials and JWT/OIDC configs.
- [ ] Pub/Sub for events between services (if you adopt event-driven).
- [ ] Observability:
  - Managed Prometheus / OpenTelemetry collector.

**DoD**
- An environment on GKE with declarative deployment, working autoscaling, and visible logs and metrics.

**Learning**: real Kubernetes, Cloud SQL, Secret Manager, Pub/Sub, IAM, Workload Identity, operations, scaling.

### 10.2 Infrastructure as code (recommended)
- [ ] Terraform for:
  - Artifact Registry repos
  - GKE cluster
  - Cloud SQL
  - Secret Manager
  - Pub/Sub topics/subscriptions
  - IAM bindings

**DoD**
- Infrastructure that can be recreated from scratch (no critical manual clicks).

**Learning**: IaC, governance, reproducibility.

---

## Phase 11 — Quality, architecture and performance

- [ ] ADRs (Architecture Decision Records) for key decisions (sync vs async, DB per service, gateway, etc.).
- [ ] Performance testing (k6/JMeter) with targets (p95 latency, error rate).
- [ ] "Chaos lite": controlled failures (timeouts, dependency outages) and validation of graceful degradation.

**DoD**
- Documented decisions + repeatable performance tests.

**Learning**: living architecture, performance, advanced resilience.

---

## Suggested path (to maximize learning)

1) Phase 0 (tests + docs)  
2) Phase 1 (modularization)  
3) Extract `wizard-service` first  
4) Add the gateway + basic security  
5) Extract `inventory-service`  
6) Extract `duel-service` + move `@Retryable` + events/outbox  
7) End-to-end observability  
8) GKE (real deployment and Kubernetes operations)  

---

## Phase X — `config-server` (centralized configuration)

Goal: move each service's configuration to a central point, with profiles and per-environment deployment.

- [ ] Create a `config-server` (Spring Cloud Config Server or an alternative) and define:
  - an `application.yml` per service (`wizard-service`, `inventory-service`, `duel-service`, `api-gateway`)
  - profiles: `local`, `dev`, `stg`, `prod`
- [ ] Storage strategy:
  - a Git configuration repo (recommended for learning) or
  - Secret Manager/Parameter Store (on GCP) for secrets
- [ ] Client integration:
  - config bootstrap in each microservice
  - controlled refresh (if applicable)
- [ ] Config security:
  - auth between the services and the config-server
  - keep secrets separate (do not store them in plain text in Git)

**DoD**
- Each microservice can start with external configuration per environment.
- Audited/versioned configuration with simple rollback.

**Learning**: distributed configuration, profiles, per-environment operations.
