## ¿Qué son las `Specifications` en Spring (Spring Data JPA)?

En **Spring Data JPA**, una *Specification* es una forma **componible** de construir criterios de búsqueda dinámicos usando el **Criteria API** de JPA, sin escribir SQL/HQL “a mano”.

En términos prácticos:

- Una `Specification<T>` representa “una condición” (un `WHERE`) para filtrar entidades `T`.
- Se puede **combinar** con otras specifications usando `.and(...)` y `.or(...)`.
- Se ejecuta a través de un repositorio que implemente `JpaSpecificationExecutor<T>`, por ejemplo:
  - `findAll(spec, pageable)`
  - `findAll(spec)`

Esto permite armar filtros opcionales según lo que el cliente envíe como parámetros en la request.

---

## ¿Cómo funciona por debajo (idea general)?

Una `Specification` se traduce a un `Predicate` del Criteria API:

- **Entity**: `Wizard`
- **Root**: representa la tabla/entidad
- **CriteriaBuilder**: utilidades para construir comparaciones (`equal`, `like`, `lower`, etc.)
- **Predicate**: condición final que se aplica como `WHERE`

---

## ¿Cómo se usan hasta el momento en este proyecto?

### 1) `WizardRepository` habilita Specifications

El repositorio implementa `JpaSpecificationExecutor<Wizard>`, lo que habilita `findAll(Specification, Pageable)`:

```10:15:wizard-service/src/main/java/com/magic/hogwarts/wizard/infrastructure/persistence/WizardRepository.java
public interface WizardRepository extends JpaRepository<Wizard, Long>, JpaSpecificationExecutor<Wizard> {
    @Override
    Page<Wizard> findAll(Specification<Wizard> spec, Pageable pageable);

    boolean existsByHouseIgnoreCaseAndNameIgnoreCase(String house, String name);
}
```

### 2) `WizardSpecifications` concentra filtros reutilizables

Actualmente hay 2 filtros:

- **house equals (case-insensitive)**
- **name contains (case-insensitive)**

```6:19:wizard-service/src/main/java/com/magic/hogwarts/wizard/infrastructure/persistence/WizardSpecifications.java
public final class WizardSpecifications {
    private WizardSpecifications() {}

    public static Specification<Wizard> houseEqualsIgnoreCase(String house) {
        return (root, query, cb) ->
                cb.equal(cb.lower(root.get("house")), house.toLowerCase());
    }

    public static Specification<Wizard> nameContainsIgnoreCase(String name) {
        String like = "%" + name.toLowerCase() + "%";
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("name")), like);
    }
}
```

#### Explicación detallada de cada línea (51–64)

- **L51: `public final class WizardSpecifications {`**
  - Declara una **clase pública** llamada `WizardSpecifications`, accesible desde cualquier paquete del proyecto.
  - El modificador **`final`** indica que **no puede heredarse** (nadie puede crear `class X extends WizardSpecifications`).
  - Este patrón se usa típicamente para **clases utilitarias**: agrupan métodos `static` (en este caso, factories de `Specification<Wizard>`) y no representan un “objeto” con estado.
  - La llave `{` abre el bloque de la clase donde se definen sus miembros (métodos, constantes, etc.).

- **L52: `private WizardSpecifications() {}`**
  - Define un **constructor** con el mismo nombre de la clase. En Java, el constructor es el mecanismo para crear instancias con `new`.
  - Al ser **`private`**, **impide** que otros (o el mismo paquete) puedan hacer `new WizardSpecifications()`.
  - El cuerpo `{}` está vacío porque **no hay inicialización que realizar**: la clase no está pensada para instanciarse.
  - En combinación con `final`, refuerza la intención: `WizardSpecifications` es un **contenedor de utilidades estáticas** (las specifications) y no una clase destinada a ser extendida o instanciada.

- **L53: (línea en blanco)**
  - Separa visualmente el constructor de los métodos `static` para mejorar legibilidad. No tiene efecto en compilación.

- **L54: `public static Specification<Wizard> houseEqualsIgnoreCase(String house) {`**
  - Declara un método **público** (usable desde fuera), **estático** (se llama como `WizardSpecifications.houseEqualsIgnoreCase(...)` sin instanciar la clase).
  - El tipo de retorno es `Specification<Wizard>`: el método **construye y devuelve** una condición reutilizable para filtrar entidades `Wizard`.
  - Recibe `String house` como valor a comparar contra el campo `house` de la entidad.
  - La llave `{` abre el cuerpo del método.

- **L55: `return (root, query, cb) ->`**
  - Devuelve una **lambda** que implementa `Specification<Wizard>` (en esencia, la función `toPredicate(root, query, cb)`).
  - Parámetros típicos del Criteria API:
    - `root`: representa la entidad/tabla (`Wizard`) y permite acceder a sus atributos con `root.get("campo")`.
    - `query`: representa la consulta JPA que se está construyendo (aquí no se modifica, solo se recibe).
    - `cb` (*CriteriaBuilder*): “fábrica” de expresiones y predicados (`equal`, `like`, `lower`, etc.).
  - El `->` indica que la lambda devolverá un `Predicate` (la condición del `WHERE`).

- **L56: `cb.equal(cb.lower(root.get("house")), house.toLowerCase());`**
  - Construye un `Predicate` de igualdad usando `cb.equal(...)`.
  - `root.get("house")` accede al atributo `house` de `Wizard` (debe coincidir con el nombre del campo/propiedad mapeada).
  - `cb.lower(...)` fuerza a minúsculas el valor del campo **en base de datos** para hacer comparación **case-insensitive**.
  - `house.toLowerCase()` convierte el input del usuario a minúsculas para compararlo en el mismo “formato”.
  - Resultado: condición equivalente a \(LOWER(house) = lower(input)\).

- **L57: `}`**
  - Cierra el método `houseEqualsIgnoreCase`.

- **L58: (línea en blanco)**
  - Separación visual entre ambos métodos de specification.

- **L59: `public static Specification<Wizard> nameContainsIgnoreCase(String name) {`**
  - Declara otro método **público y estático** que devuelve una `Specification<Wizard>`.
  - Se usará para filtrar por el campo `name` de `Wizard` usando una búsqueda “contiene” (*contains*) sin distinguir mayúsculas/minúsculas.

- **L60: `String like = "%" + name.toLowerCase() + "%";`**
  - Prepara el patrón para un `LIKE` SQL:
    - `%` significa “cualquier secuencia de caracteres” (comodín).
  - Al envolver el texto con `%...%`, la búsqueda equivale a “**contiene**”.
  - `name.toLowerCase()` normaliza el input a minúsculas para comparación case-insensitive.

- **L61: `return (root, query, cb) ->`**
  - Devuelve una lambda `Specification` igual que en L55, usando `root`, `query` y `cb` para construir el `Predicate`.

- **L62: `cb.like(cb.lower(root.get("name")), like);`**
  - Construye un `Predicate` con `cb.like(...)` (coincidencia por patrón).
  - `root.get("name")` accede al atributo `name` de la entidad.
  - `cb.lower(...)` baja a minúsculas el valor del campo en la BD para comparación sin caso.
  - `like` es el patrón `%<texto>%` en minúsculas.
  - Resultado: condición equivalente a \(LOWER(name) LIKE '%texto%'\).

- **L63: `}`**
  - Cierra el método `nameContainsIgnoreCase`.

- **L64: `}`**
  - Cierra la clase `WizardSpecifications`.

### 3) `WizardService.list(...)` arma el filtro dinámicamente

El endpoint `GET /api/wizards` permite `house` y `name` como filtros opcionales. El servicio decide:

- Si **no** hay filtros → se usa `repository.findAll(pageable)` (sin specification).
- Si hay filtros → se arma `spec` y se usa `repository.findAll(spec, pageable)`.

Esto evita un problema común: **pasar `null` como `Specification` a `findAll(spec, pageable)`**, lo que puede lanzar `IllegalArgumentException` con mensajes como `"Specification must not be null"`.

---

## Ventajas de usar Specifications

- **Filtros dinámicos** sin ifs complejos ni concatenación de strings.
- **Composición**: podés combinar condiciones de forma declarativa.
- **Reutilización**: las specs viven en una clase tipo `*Specifications` y se usan desde servicios.
- **Paginación**: funciona muy bien con `Pageable`.

---

## Recomendaciones para seguir escalando

- **Agregar más specifications** por campo (por ejemplo, `level >=`, `level <=`, `house in`, etc.).
- Para muchos filtros, considerar un DTO de filtros (por ej. `WizardFilter`) y construir la spec desde ahí.
- Evitar bajar `toLowerCase()` sin controlar `Locale` si se vuelve crítico (en general está OK para este caso).

