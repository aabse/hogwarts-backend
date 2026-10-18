# DTOs (Data Transfer Objects) en este proyecto

## ¿Qué es un DTO?

Un **DTO (Data Transfer Object)** es un objeto simple cuyo propósito es **transportar datos** entre capas (normalmente entre la API y la lógica de negocio) sin exponer directamente:

- Las **entidades del dominio** (modelos JPA, estado interno, relaciones, etc.).
- Detalles de persistencia o implementación.
- Campos que no deberían viajar por la red (por seguridad, acoplamiento o estabilidad del contrato).

Un DTO suele ser una clase “plana” (con campos + getters/setters) y se usa para definir el **contrato de entrada/salida** de los endpoints.

---

## ¿Cómo se usan los DTO en este proyecto?

Este repositorio tiene 3 microservicios Gradle (multi-módulo):

- `wizard-service`
- `inventory-service`
- `duel-service`

En los 3 se sigue el mismo patrón:

- **DTOs de request**: en `.../api/dto/request`
- **DTOs de response**: en `.../api/dto/response`

Los **Controllers** reciben/envían DTOs, y los **Services** hacen el mapeo entre DTO ↔ modelo de dominio.

### Flujo típico

1. **Controller**
   - Recibe JSON en un DTO de request con `@RequestBody`.
   - Valida el payload con `@Valid` (Jakarta Bean Validation).
2. **Service (capa de aplicación)**
   - Convierte el DTO de request a un modelo de dominio (por ejemplo `Wizard`, `Wand`, `Duel`).
   - Ejecuta reglas de negocio.
   - Persiste/consulta mediante el repositorio.
   - Convierte el modelo de dominio a un DTO de response (por ejemplo con un método `toDTO(...)`).
3. **Controller**
   - Devuelve el DTO de response como JSON.

---

## Estructura y ejemplos por microservicio

### 1) `wizard-service`

**DTOs**

- Requests:
  - `wizard-service/src/main/java/com/magic/hogwarts/wizard/api/dto/request/WizardRequestDTO.java`
  - `wizard-service/src/main/java/com/magic/hogwarts/wizard/api/dto/request/WizardPatchRequestDTO.java`
  - `wizard-service/src/main/java/com/magic/hogwarts/wizard/api/dto/request/AttackRequestDTO.java`
- Responses:
  - `wizard-service/src/main/java/com/magic/hogwarts/wizard/api/dto/response/WizardDTO.java`
  - `wizard-service/src/main/java/com/magic/hogwarts/wizard/api/dto/response/AttackResultDTO.java`

**Cómo se usan**

- `WizardController` expone endpoints como:
  - `POST /api/wizards` recibe `WizardRequestDTO` y devuelve `WizardDTO`.
  - `PATCH /api/wizards/{id}` recibe `WizardPatchRequestDTO` y devuelve `WizardDTO`.
  - `POST /api/wizards/attack` recibe `AttackRequestDTO` y devuelve `AttackResultDTO`.
- `WizardService`:
  - Construye un `Wizard` (dominio) desde `WizardRequestDTO`.
  - Aplica reglas (por ejemplo, asignación de casa si viene vacía, unicidad por (house, name), etc.).
  - Mapea el resultado a `WizardDTO` con un método privado `toDTO(Wizard)`.

**Validación en el borde (API)**

Los DTOs de request usan anotaciones como `@NotBlank`, `@Size`, `@Pattern`, `@NotNull` y `@Positive`.
Esto hace que la validación ocurra antes de entrar a la lógica de negocio.

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

**Cómo se usan**

- Controllers (`WandController`, `PotionController`, `BroomController`) trabajan con DTOs en la firma de los endpoints.
- Ejemplo claro:
  - `POST /api/inventory/wands` recibe `WandCreateRequestDTO` y devuelve `WandDTO`.
  - `POST /api/inventory/wands/{wandId}/assign` recibe `AssignWandRequestDTO` y devuelve `WandDTO`.
- `WandService`:
  - Construye `Wand` desde `WandCreateRequestDTO`.
  - Aplica reglas de asignación (evita reasignación conflictiva).
  - Mapea `Wand` a `WandDTO` con `toDTO(Wand)`.

---

### 3) `duel-service`

**DTOs**

- Requests:
  - `duel-service/src/main/java/com/magic/hogwarts/duel/api/dto/request/CreateDuelRequestDTO.java`
- Responses:
  - `duel-service/src/main/java/com/magic/hogwarts/duel/api/dto/response/DuelDTO.java`

**Cómo se usan**

- `DuelController`:
  - `POST /api/duels` recibe `CreateDuelRequestDTO` y devuelve `DuelDTO`.
  - `GET /api/duels/{id}` devuelve `DuelDTO`.
- `DuelService`:
  - Construye la entidad de dominio `Duel` desde el request.
  - Resuelve el resultado y lo persiste.
  - Devuelve un `DuelDTO` mapeado desde la entidad con `toDTO(Duel)`.

Nota: `DuelDTO` incluye un campo `debugSeed` para reproducibilidad/debug del resultado (dato útil para clientes o troubleshooting).

---

## ¿Por qué esta aproximación es útil aquí?

- **Contrato estable de API**: cambios en entidades JPA no rompen necesariamente el JSON público.
- **Seguridad y control**: solo se exponen campos permitidos; se ocultan detalles internos.
- **Validación centralizada**: los requests se validan con Bean Validation en el borde (`@Valid` en controllers).
- **Separación de responsabilidades**:
  - `api` (Controllers + DTOs): HTTP/JSON y contrato.
  - `application` (Services): casos de uso y reglas.
  - `domain` (model): representación del negocio/persistencia.
  - `infrastructure/persistence`: repositorios y specs.

---

## Convenciones usadas en los DTOs de este repo

- Se diferencian explícitamente por carpeta:
  - `dto/request` (entrada)
  - `dto/response` (salida)
- Se usa **Lombok** (`@Data`) para reducir boilerplate (getters/setters).
- Los DTOs de request suelen llevar **anotaciones de validación**.
- El mapeo se hace **manual** en los servicios (métodos `toDTO(...)`), en lugar de usar mappers tipo MapStruct (por ahora).

