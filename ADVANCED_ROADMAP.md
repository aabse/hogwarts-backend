# ADVANCED_ROADMAP — Spring Boot (nivel avanzado) + Microservicios + GCP (GKE)

Este documento propone un backlog/roadmap **orientado a aprendizaje profundo** de Spring y a una evolución realista hacia **microservicios por dominio** con despliegue en **GCP**.  
La idea es que cada épica te obligue a tocar temas “de verdad”: consistencia, resiliencia, testing serio, observabilidad, seguridad, CI/CD, infraestructura.

---

## Principios y objetivos

- **Microservicios**:
  - `wizard-service`: gestión de magos, niveles y casas.
  - `duel-service`: lógica de batallas/duelos (**acá vive el retry con `@Retryable`**).
  - `inventory-service`: gestión de inventario (varitas, pociones y escobas).
  - `config-server`: configuración centralizada.
  - `api-gateway`: punto de entrada único + seguridad (JWT).
- **API-first**: contratos claros, versionado, errores consistentes.
- **Confiabilidad**: timeouts, retries con criterio, idempotencia, tolerancia a fallos.
- **Observabilidad por defecto**: logs estructurados + trazas + métricas.
- **Seguridad desde el inicio**: authn/authz, secretos, least privilege.
- **Evolución incremental**: primero modularizar el monolito, luego extraer servicios.

---

## Fase 0 — Baseline del proyecto (calidad mínima profesional)

### 0.1 Estándares y documentación
- [ ] **README** con cómo correr local, endpoints, y estructura del dominio.
- [ ] **Convención de commits**, branching, y versionado.
- [ ] **OpenAPI** para endpoints (Swagger UI) y ejemplo de requests/responses.

**Definition of Done (DoD)**  
- El proyecto se ejecuta local en 1 comando (o 2 como máximo) y está documentado.
- Existe documentación de API y errores típicos (400/404/409).

**Aprendizaje**: Spring Web, validación, diseño de API, DX.

### 0.2 Testing serio (no sólo “context loads”)
- [ ] Unit tests de servicios (reglas de negocio).
- [ ] WebMvcTest para controllers + validación.
- [ ] Integración con DB real via **Testcontainers (Postgres)** (evitar divergencia H2 vs prod).
- [ ] Cobertura mínima por módulo y criterios de calidad (no como número, sino por rutas críticas).

**DoD**  
- CI falla si tests críticos fallan.
- Hay tests para “happy path” y “edge cases”.

**Aprendizaje**: Spring Test, Testcontainers, slicing tests.

---

## Fase 1 — Modularización previa (pre-microservicios)

Objetivo: preparar el código para dividirse sin dolor.

- [ ] Reorganizar paquetes por dominio (por ejemplo):
  - `com.magic.hogwarts.wizard.*`
  - `com.magic.hogwarts.inventory.*`
  - `com.magic.hogwarts.duel.*`
- [ ] Definir **DTOs** por capa (request/response), evitando exponer entidades JPA.
- [ ] Unificar respuesta de errores (código, mensaje, timestamp, path, traceId).
- [ ] Agregar paginación/filtros en listados (Spring Data).

**DoD**
- No hay referencias circulares entre dominios.
- Controllers no devuelven entidades JPA directamente.

**Aprendizaje**: arquitectura por dominios, separación de capas, contratos.

---

## Fase 2 — `wizard-service` (microservicio 1)

### 2.1 Extracción del servicio
- [ ] Convertir el proyecto a **multi-módulo Gradle** (o repos separados) y extraer `wizard-service`.
- [ ] Definir boundaries: el servicio es dueño de `Wizard` y su DB.
- [ ] Crear endpoints completos:
  - `POST /api/wizards`
  - `GET /api/wizards` (paginado + filtros)
  - `GET /api/wizards/{id}`
  - `PATCH /api/wizards/{id}` (parcial)
  - `DELETE /api/wizards/{id}` (si aplica)
- [ ] Enriquecer el modelo con reglas (ej.: nombre único por casa, límites de nivel, etc.).

**DoD**
- Un `wizard-service` se ejecuta solo y pasa su suite de tests.
- Contrato OpenAPI estable y versionado (v1).

**Aprendizaje**: modularización, DDD-lite, API design.

### 2.2 Persistencia “de producción”
- [ ] Migraciones con **Flyway** o **Liquibase**.
- [ ] Constraints e índices (unicidad, not-null, índices de búsqueda).
- [ ] Separar “read model” vs “write model” cuando sea útil (proyección/DTO optimizado).

**DoD**
- La DB se recrea desde cero sólo con migraciones.
- Las queries principales están indexadas y justificadas.

**Aprendizaje**: JPA/Hibernate, SQL real, migraciones.

### 2.3 Concurrencia, idempotencia y consistencia
- [ ] Mantener **optimistic locking** (`@Version`) y **retry** con backoff.
- [ ] Introducir **idempotency key** en operaciones de escritura críticas (si aplica).
- [ ] Validar invariantes ante concurrencia (tests con múltiples hilos / escenarios).

**DoD**
- Se puede simular un conflicto y el sistema responde 409 o reintenta correctamente.

**Aprendizaje**: concurrencia, patrones de consistencia.

### 2.4 Resiliencia interna (dentro del servicio)
- [ ] Timeouts consistentes en llamadas externas (si las hubiera).
- [ ] Circuit breaker/bulkhead para dependencias (si agregas integraciones).

**Aprendizaje**: resiliencia (retry con criterio, fallos).

---

## Fase 3 — `inventory-service` (microservicio 2)

### 3.1 Extracción y modelo de inventario
- [ ] Extraer `inventory-service` como dueño de:
  - `Wand` (varitas)
  - `Potion` (pociones)
  - `Broom` (escobas)
- [ ] Reglas avanzadas: stock, disponibilidad, asignación, auditoría de cambios.
- [ ] Endpoints:
  - `POST /api/inventory/wands`
  - `GET /api/inventory/wands` (paginado + filtros)
  - `POST /api/inventory/potions`
  - `GET /api/inventory/potions` (paginado + filtros)
  - `POST /api/inventory/brooms`
  - `GET /api/inventory/brooms` (paginado + filtros)
  - `POST /api/inventory/wands/{wandId}/assign` (asignar a wizard)

**Decisión arquitectónica (aprendizaje clave)**  
¿La relación wizard↔wand se resuelve con:
- **(A) Referencias por ID** (cada servicio guarda el ID del otro) + validación por API/eventos, o
- **(B) Event-driven** (inventory-service emite “wand-assigned” y wizard-service materializa un read model)?

**DoD**
- El servicio opera independientemente, con migraciones y tests.
- Contrato API con versionado.

**Aprendizaje**: boundaries, modelado, consistencia entre servicios.

---

## Fase 4 — `duel-service` (microservicio 3)

### 4.1 Orquestación de duelos
- [ ] Extraer `duel-service` como dueño de `Duel` (historial, estado, timestamps, participantes).
- [ ] Endpoint:
  - `POST /api/duels` (crea un duelo: attackerId, defenderId)
  - `GET /api/duels/{id}` (estado + resultado)
- [ ] Decisión del resultado en servidor (por nivel, aleatorio, reglas, etc.).
- [ ] Mover/centralizar acá la lógica de **retry** (`@Retryable`) aplicada a efectos del duelo.
- [ ] Auditoría: guardar evento/resultado, permitir “replay”/debug.

**DoD**
- Cada duelo genera un registro persistente y reproducible (misma entrada → mismo resultado si se define).

**Aprendizaje**: orquestación, auditoría, diseño de flujos.

### 4.2 Consistencia cross-service (patrones avanzados)
- [ ] Implementar **Saga (orchestration)** si el duelo impacta a varios servicios.
- [ ] Implementar **Transactional Outbox** para publicar eventos de forma confiable.

**DoD**
- No hay “double updates” ni eventos perdidos ante caídas.

**Aprendizaje**: outbox, sagas, consistencia eventual.

---

## Fase 5 — Comunicación entre microservicios

### 5.1 Síncrona (REST)
- [ ] Timeouts por defecto, retries limitados, circuit breaker.
- [ ] Versionado de APIs y compatibilidad hacia atrás.
- [ ] Contract testing (consumer-driven) entre servicios.

### 5.2 Asíncrona (eventos)
- [ ] Definir un catálogo de eventos:
  - `wizard-created`
  - `wizard-leveled-up`
  - `inventory-wand-created`
  - `inventory-wand-assigned`
  - `inventory-potion-created`
  - `inventory-broom-created`
  - `duel-finished`
- [ ] Diseñar eventos con:
  - `eventId`, `eventType`, `occurredAt`, `schemaVersion`, `payload`
  - idempotencia del consumidor (deduplicación).

**DoD**
- Consumidores idempotentes + replay controlado de eventos.

**Aprendizaje**: event-driven, schemas, idempotencia distribuida.

---

## Fase 6 — `api-gateway` (Edge)

- [ ] Agregar `api-gateway` (ej.: Spring Cloud Gateway) para:
  - routing por servicio
  - rate limiting
  - authn/authz centralizado (JWT)
  - correlation-id y headers comunes
- [ ] Versionar rutas externas (`/api/v1/...`) y mapear a servicios internos.

**DoD**
- El cliente habla con el gateway; los servicios pueden estar privados.

**Aprendizaje**: edge patterns, seguridad centralizada.

---

## Fase 7 — Seguridad (avanzada)

- [ ] OAuth2/OIDC con JWT:
  - roles/authorities (ej.: `wizard:read`, `wizard:write`, `duel:execute`)
  - validación en gateway y/o en cada servicio
- [ ] Gestión de secretos:
  - local: `.env` / vault local (sin commitear)
  - cloud: GCP Secret Manager
- [ ] Protección ante abuso:
  - rate limit en gateway
  - límites de payload, timeouts, headers.

**DoD**
- Rutas protegidas, permisos por rol, y tests de seguridad básicos.

**Aprendizaje**: Spring Security, OIDC, hardening.

---

## Fase 8 — Observabilidad (operación real)

- [ ] Logging estructurado (JSON) + `traceId`/`correlationId`.
- [ ] Tracing distribuido con **OpenTelemetry** (propagar context entre servicios).
- [ ] Métricas con **Micrometer** (latencia, errores, saturación).
- [ ] Health: liveness/readiness, indicadores de DB/broker.

**DoD**
- Puedes seguir una request end-to-end (gateway → service → service) con trace.

**Aprendizaje**: o11y, trazas, métricas, debugging.

---

## Fase 9 — Plataforma local + CI/CD

### 9.1 Contenerización
- [ ] `Dockerfile` por servicio (multi-stage, usuario no-root).
- [ ] `docker-compose` para dev:
  - Postgres por servicio (o uno con schemas separados al inicio)
  - (más adelante) broker/event bus

### 9.2 CI/CD (GitHub Actions o Cloud Build)
- [ ] Pipeline: build + tests + análisis (SAST) + SBOM + escaneo de imágenes.
- [ ] Versionado de imagen y promoción por ambientes (dev/stg/prod).

**DoD**
- Build reproducible, imágenes firmadas/escaneadas, y despliegue automatizado.

**Aprendizaje**: DevOps, supply chain, automatización.

---

## Fase 10 — GCP hands-on (GKE)

### 10.1 GKE
- [ ] Crear proyecto GCP y habilitar APIs:
  - Artifact Registry
  - GKE
  - Cloud SQL
  - Secret Manager
  - Pub/Sub
  - Cloud Logging/Monitoring
- [ ] Artifact Registry:
  - repo Docker para imágenes
- [ ] Elegir packaging:
  - Helm charts por servicio, o
  - Kustomize overlays por ambiente
- [ ] Deploy a GKE (uno por servicio):
  - namespaces por ambiente/equipo
  - HPA
  - PodDisruptionBudget
  - requests/limits
- [ ] Identity:
  - Workload Identity (evitar keys)
- [ ] Ingress + routing (o gateway en cluster).
- [ ] Cloud SQL (Postgres) por servicio (recomendado) + Cloud SQL Auth Proxy como sidecar.
- [ ] Secret Manager para credenciales y JWT/OIDC configs.
- [ ] Pub/Sub para eventos entre servicios (si adoptas event-driven).
- [ ] Observabilidad:
  - Managed Prometheus / OpenTelemetry collector.

**DoD**
- Un entorno en GKE con despliegue declarativo, autoscaling funcional, logs y métricas visibles.

**Aprendizaje**: Kubernetes real, Cloud SQL, Secret Manager, Pub/Sub, IAM, Workload Identity, operaciones, escalado.

### 10.2 Infra como código (recomendado)
- [ ] Terraform para:
  - repos Artifact Registry
  - cluster GKE
  - Cloud SQL
  - Secret Manager
  - Pub/Sub topics/subscriptions
  - IAM bindings

**DoD**
- Infra recreable desde cero (sin clicks manuales críticos).

**Aprendizaje**: IaC, gobernanza, reproducibilidad.

---

## Fase 11 — Calidad, arquitectura y performance

- [ ] ADRs (Architecture Decision Records) por decisiones clave (sync vs async, DB per service, gateway, etc.).
- [ ] Performance testing (k6/JMeter) con objetivos (p95 latency, error rate).
- [ ] “Chaos lite”: fallos controlados (timeouts, caídas de dependencia) y validación de degradación.

**DoD**
- Decisiones documentadas + pruebas de performance repetibles.

**Aprendizaje**: arquitectura viva, performance, resiliencia avanzada.

---

## Ruta sugerida (para maximizar aprendizaje)

1) Fase 0 (tests + docs)  
2) Fase 1 (modularización)  
3) Extraer `wizard-service` primero  
4) Agregar gateway + seguridad básica  
5) Extraer `inventory-service`  
6) Extraer `duel-service` + mover `@Retryable` + eventos/outbox  
7) Observabilidad end-to-end  
8) GKE (deploy real y operación Kubernetes)  

---

## Fase X — `config-server` (Configuración centralizada)

Objetivo: extraer la configuración de cada servicio a un punto central, con perfiles y despliegue por ambiente.

- [ ] Crear `config-server` (Spring Cloud Config Server o alternativa) y definir:
  - `application.yml` por servicio (`wizard-service`, `inventory-service`, `duel-service`, `api-gateway`)
  - perfiles: `local`, `dev`, `stg`, `prod`
- [ ] Estrategia de storage:
  - repo Git de configuración (recomendado para aprendizaje) o
  - Secret Manager/Parameter Store (en GCP) para secretos
- [ ] Integración de clientes:
  - bootstrap de config en cada microservicio
  - refresh controlado (si aplica)
- [ ] Seguridad del config:
  - auth entre servicios y config-server
  - separar secretos (no guardarlos en texto plano en Git)

**DoD**
- Cada microservicio puede arrancar con config externa por ambiente.
- Config auditada/versionada y con rollback simple.

**Aprendizaje**: configuración distribuida, perfiles, operación por ambiente.

