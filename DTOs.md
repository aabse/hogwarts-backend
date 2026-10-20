# DTOs (Data Transfer Objects) in this project

## What is a DTO?

A **DTO (Data Transfer Object)** is a simple object whose purpose is to **carry data** between layers (usually between the API and the business logic) without directly exposing:

- The **domain entities** (JPA models, internal state, relationships, etc.).
- Persistence or implementation details.
- Fields that should not travel over the network (for security, coupling or contract stability reasons).

A DTO is usually a "flat" class (fields + getters/setters) and is used to define the **input/output contract** of the endpoints.

---

## How are DTOs used in this project?

This repository has 3 Gradle microservices (multi-module):

- `wizard-service`
- `inventory-service`
- `duel-service`

All 3 follow the same pattern:

- **Request DTOs**: in `.../api/dto/request`
- **Response DTOs**: in `.../api/dto/response`

The **Controllers** receive/send DTOs, and the **Services** do the mapping between DTO ↔ domain model.

### Typical flow

1. **Controller**
   - Receives JSON into a request DTO with `@RequestBody`.
   - Validates the payload with `@Valid` (Jakarta Bean Validation).
2. **Service (application layer)**
   - Converts the request DTO into a domain model (for example `Wizard`, `Wand`, `Duel`).
   - Runs the business rules.
   - Persists/queries through the repository.
   - Converts the domain model into a response DTO (for example with a `toDTO(...)` method).
3. **Controller**
   - Returns the response DTO as JSON.

---

## Structure and examples per microservice

### 1) `wizard-service`

**DTOs**

- Requests:
  - `wizard-service/src/main/java/com/magic/hogwarts/wizard/api/dto/request/WizardRequestDTO.java`
  - `wizard-service/src/main/java/com/magic/hogwarts/wizard/api/dto/request/WizardPatchRequestDTO.java`
  - `wizard-service/src/main/java/com/magic/hogwarts/wizard/api/dto/request/AttackRequestDTO.java`
- Responses:
  - `wizard-service/src/main/java/com/magic/hogwarts/wizard/api/dto/response/WizardDTO.java`
  - `wizard-service/src/main/java/com/magic/hogwarts/wizard/api/dto/response/AttackResultDTO.java`

**How they are used**

- `WizardController` exposes endpoints such as:
  - `POST /api/wizards` receives `WizardRequestDTO` and returns `WizardDTO`.
  - `PATCH /api/wizards/{id}` receives `WizardPatchRequestDTO` and returns `WizardDTO`.
  - `POST /api/wizards/attack` receives `AttackRequestDTO` and returns `AttackResultDTO`.
- `WizardService`:
  - Builds a `Wizard` (domain) from `WizardRequestDTO`.
  - Applies rules (for example, assigning a house if it comes empty, uniqueness by (house, name), etc.).
  - Maps the result to `WizardDTO` with a private `toDTO(Wizard)` method.

**Validation at the edge (API)**

Request DTOs use annotations such as `@NotBlank`, `@Size`, `@Pattern`, `@NotNull` and `@Positive`.
This makes validation happen before entering the business logic.

---

### 2) `inventory-service`

**DTOs**

- Requests:
  - `inventory-service/src/main/java/com/magic/hogwarts/inventory/api/dto/request/WandCreateRequestDTO.java`
  - `inventory-service/src/main/java/com/magic/hogwarts/inventory/api/dto/request/AssignWandRequestDTO.java`
  - `inventory-service/src/main/java/com/magic/hogwarts/inventory/api/dto/request/PotionCreateRequestDTO.java`
  - `inventory-service/src/main/java/com/magic/hogwarts/inventory/api/dto/request/BroomCreateRequestDTO.java`
- Responses:
  - `inventory-service/src/main/java/com/magic/hogwarts/inventory/api/dto/response/WandDTO.java`
  - `inventory-service/src/main/java/com/magic/hogwarts/inventory/api/dto/response/PotionDTO.java`
  - `inventory-service/src/main/java/com/magic/hogwarts/inventory/api/dto/response/BroomDTO.java`

**How they are used**

- The controllers (`WandController`, `PotionController`, `BroomController`) work with DTOs in the endpoint signatures.
- A clear example:
  - `POST /api/inventory/wands` receives `WandCreateRequestDTO` and returns `WandDTO`.
  - `POST /api/inventory/wands/{wandId}/assign` receives `AssignWandRequestDTO` and returns `WandDTO`.
- `WandService`:
  - Builds a `Wand` from `WandCreateRequestDTO`.
  - Applies assignment rules (prevents conflicting reassignment).
  - Maps `Wand` to `WandDTO` with `toDTO(Wand)`.

---

### 3) `duel-service`

**DTOs**

- Requests:
  - `duel-service/src/main/java/com/magic/hogwarts/duel/api/dto/request/CreateDuelRequestDTO.java`
- Responses:
  - `duel-service/src/main/java/com/magic/hogwarts/duel/api/dto/response/DuelDTO.java`

**How they are used**

- `DuelController`:
  - `POST /api/duels` receives `CreateDuelRequestDTO` and returns `DuelDTO`.
  - `GET /api/duels/{id}` returns `DuelDTO`.
- `DuelService`:
  - Builds the `Duel` domain entity from the request.
  - Resolves the outcome and persists it.
  - Returns a `DuelDTO` mapped from the entity with `toDTO(Duel)`.

Note: `DuelDTO` includes a `debugSeed` field for reproducibility/debugging of the outcome (useful data for clients or troubleshooting).

---

## Why is this approach useful here?

- **Stable API contract**: changes to JPA entities do not necessarily break the public JSON.
- **Security and control**: only allowed fields are exposed; internal details are hidden.
- **Centralized validation**: requests are validated with Bean Validation at the edge (`@Valid` in controllers).
- **Separation of concerns**:
  - `api` (Controllers + DTOs): HTTP/JSON and contract.
  - `application` (Services): use cases and rules.
  - `domain` (model): business/persistence representation.
  - `infrastructure/persistence`: repositories and specs.

---

## Conventions used in this repo's DTOs

- They are explicitly separated by folder:
  - `dto/request` (input)
  - `dto/response` (output)
- **Lombok** (`@Data`) is used to reduce boilerplate (getters/setters).
- Request DTOs usually carry **validation annotations**.
- Mapping is done **by hand** in the services (`toDTO(...)` methods), instead of using mappers like MapStruct (for now).
