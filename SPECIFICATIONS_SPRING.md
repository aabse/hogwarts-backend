## What are `Specifications` in Spring (Spring Data JPA)?

In **Spring Data JPA**, a *Specification* is a **composable** way to build dynamic search criteria using the JPA **Criteria API**, without writing SQL/HQL "by hand".

In practical terms:

- A `Specification<T>` represents "a condition" (a `WHERE`) to filter `T` entities.
- It can be **combined** with other specifications using `.and(...)` and `.or(...)`.
- It is executed through a repository that implements `JpaSpecificationExecutor<T>`, for example:
  - `findAll(spec, pageable)`
  - `findAll(spec)`

This lets you build optional filters depending on what the client sends as request parameters.

---

## How does it work underneath (general idea)?

A `Specification` is translated into a Criteria API `Predicate`:

- **Entity**: `Wizard`
- **Root**: represents the table/entity
- **CriteriaBuilder**: utilities to build comparisons (`equal`, `like`, `lower`, etc.)
- **Predicate**: the final condition applied as the `WHERE`

---

## How are they used so far in this project?

### 1) `WizardRepository` enables Specifications

The repository implements `JpaSpecificationExecutor<Wizard>`, which enables `findAll(Specification, Pageable)`:

```10:15:wizard-service/src/main/java/com/magic/hogwarts/wizard/infrastructure/persistence/WizardRepository.java
public interface WizardRepository extends JpaRepository<Wizard, Long>, JpaSpecificationExecutor<Wizard> {
    @Override
    Page<Wizard> findAll(Specification<Wizard> spec, Pageable pageable);

    boolean existsByHouseIgnoreCaseAndNameIgnoreCase(String house, String name);
}
```

### 2) `WizardSpecifications` groups reusable filters

There are currently 2 filters:

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

#### Line-by-line explanation (51–64)

- **L51: `public final class WizardSpecifications {`**
  - Declares a **public class** named `WizardSpecifications`, accessible from any package in the project.
  - The **`final`** modifier means it **cannot be subclassed** (nobody can write `class X extends WizardSpecifications`).
  - This pattern is typically used for **utility classes**: they group `static` methods (in this case, `Specification<Wizard>` factories) and do not represent an "object" with state.
  - The `{` brace opens the class block where its members (methods, constants, etc.) are defined.

- **L52: `private WizardSpecifications() {}`**
  - Defines a **constructor** with the same name as the class. In Java, the constructor is the mechanism for creating instances with `new`.
  - Being **`private`**, it **prevents** others (or the same package) from calling `new WizardSpecifications()`.
  - The body `{}` is empty because **there is no initialization to do**: the class is not meant to be instantiated.
  - Combined with `final`, it reinforces the intent: `WizardSpecifications` is a **container of static utilities** (the specifications), not a class meant to be extended or instantiated.

- **L53: (blank line)**
  - Visually separates the constructor from the `static` methods to improve readability. It has no effect on compilation.

- **L54: `public static Specification<Wizard> houseEqualsIgnoreCase(String house) {`**
  - Declares a **public** (usable from outside), **static** method (called as `WizardSpecifications.houseEqualsIgnoreCase(...)` without instantiating the class).
  - The return type is `Specification<Wizard>`: the method **builds and returns** a reusable condition to filter `Wizard` entities.
  - It receives `String house` as the value to compare against the entity's `house` field.
  - The `{` brace opens the method body.

- **L55: `return (root, query, cb) ->`**
  - Returns a **lambda** that implements `Specification<Wizard>` (essentially, the `toPredicate(root, query, cb)` function).
  - Typical Criteria API parameters:
    - `root`: represents the entity/table (`Wizard`) and gives access to its attributes with `root.get("field")`.
    - `query`: represents the JPA query being built (it is not modified here, only received).
    - `cb` (*CriteriaBuilder*): a "factory" of expressions and predicates (`equal`, `like`, `lower`, etc.).
  - The `->` indicates that the lambda will return a `Predicate` (the `WHERE` condition).

- **L56: `cb.equal(cb.lower(root.get("house")), house.toLowerCase());`**
  - Builds an equality `Predicate` using `cb.equal(...)`.
  - `root.get("house")` accesses the `house` attribute of `Wizard` (it must match the name of the mapped field/property).
  - `cb.lower(...)` lowercases the field's value **in the database** to make the comparison **case-insensitive**.
  - `house.toLowerCase()` lowercases the user's input so both sides are compared in the same "format".
  - Result: a condition equivalent to \(LOWER(house) = lower(input)\).

- **L57: `}`**
  - Closes the `houseEqualsIgnoreCase` method.

- **L58: (blank line)**
  - Visual separation between the two specification methods.

- **L59: `public static Specification<Wizard> nameContainsIgnoreCase(String name) {`**
  - Declares another **public static** method that returns a `Specification<Wizard>`.
  - It is used to filter by the `Wizard`'s `name` field with a case-insensitive "contains" search.

- **L60: `String like = "%" + name.toLowerCase() + "%";`**
  - Prepares the pattern for an SQL `LIKE`:
    - `%` means "any sequence of characters" (wildcard).
  - Wrapping the text in `%...%` makes the search equivalent to "**contains**".
  - `name.toLowerCase()` normalizes the input to lowercase for a case-insensitive comparison.

- **L61: `return (root, query, cb) ->`**
  - Returns a `Specification` lambda just like in L55, using `root`, `query` and `cb` to build the `Predicate`.

- **L62: `cb.like(cb.lower(root.get("name")), like);`**
  - Builds a `Predicate` with `cb.like(...)` (pattern matching).
  - `root.get("name")` accesses the entity's `name` attribute.
  - `cb.lower(...)` lowercases the field's value in the DB for a case-insensitive comparison.
  - `like` is the lowercase `%<text>%` pattern.
  - Result: a condition equivalent to \(LOWER(name) LIKE '%text%'\).

- **L63: `}`**
  - Closes the `nameContainsIgnoreCase` method.

- **L64: `}`**
  - Closes the `WizardSpecifications` class.

### 3) `WizardService.list(...)` builds the filter dynamically

The `GET /api/wizards` endpoint accepts `house` and `name` as optional filters. The service decides:

- If there are **no** filters → it uses `repository.findAll(pageable)` (no specification).
- If there are filters → it builds `spec` and uses `repository.findAll(spec, pageable)`.

This avoids a common problem: **passing `null` as the `Specification` to `findAll(spec, pageable)`**, which can throw an `IllegalArgumentException` with messages like `"Specification must not be null"`.

---

## Advantages of using Specifications

- **Dynamic filters** without complex ifs or string concatenation.
- **Composition**: you can combine conditions declaratively.
- **Reuse**: the specs live in a `*Specifications`-style class and are used from the services.
- **Pagination**: it works very well with `Pageable`.

---

## Recommendations for further scaling

- **Add more specifications** per field (for example `level >=`, `level <=`, `house in`, etc.).
- For many filters, consider a filter DTO (e.g. `WizardFilter`) and build the spec from it.
- Avoid calling `toLowerCase()` without controlling the `Locale` if it becomes critical (in general it is fine for this case).
