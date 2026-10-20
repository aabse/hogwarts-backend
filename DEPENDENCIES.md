# Dependencies in this project (Java + Spring Boot + Gradle)

This project (`hogwarts`) is a **Spring Boot** application built with **Gradle**. The dependency "story" is defined mainly in `build.gradle` and resolved from Maven repositories (by default, **Maven Central**).

## What is a dependency?

A dependency is an external library that your code uses to compile and/or run. In Gradle (Java), they are declared with Maven coordinates:

- **groupId:artifactId:version** (for example `org.springframework.retry:spring-retry`)

Gradle downloads those libraries (and their "child" dependencies) and puts them on the compile/runtime/test **classpath** depending on the "scope" you choose.

## Where are they declared in this project?

In `build.gradle`:

- **Plugins**:
  - `org.springframework.boot` (`4.0.1`): integrates Spring Boot into the build (run, packaging, etc.).
  - `io.spring.dependency-management` (`1.1.7`): applies **dependency management** (version alignment through a BOM).
  - `java`: standard Java support.
- **Repositories**:
  - `mavenCentral()`: where the dependencies are downloaded from.
- **Dependencies**:
  - Declared inside `dependencies { ... }`.

## Spring Boot, "starters" and versions (BOM / dependency management)

Spring Boot is usually used with **starters** ("bundle" dependencies that bring a consistent set of libraries).

This project has starters such as:

- `org.springframework.boot:spring-boot-starter-data-jpa`
- `org.springframework.boot:spring-boot-starter-webmvc`
- `org.springframework.boot:spring-boot-starter-validation`

The important part: `build.gradle` **does not specify versions** for most Spring Boot dependencies. That works because:

- The Spring Boot plugin and/or `io.spring.dependency-management` apply a **BOM** (Bill Of Materials) that pins versions that are compatible with each other.
- Result: you avoid "version hell" (incompatible mixes) and keep the ecosystem aligned.

## Transitive dependencies (the "tree")

When you add a starter, Gradle also brings in **transitive dependencies**. Example: `spring-boot-starter-webmvc` brings Spring MVC and, typically, things like Jackson, logging, etc. (depending on the starter's configuration and the Boot version).

You can inspect the tree with:

```bash
./gradlew dependencies
```

And to understand why a library arrived (or which version was chosen):

```bash
./gradlew dependencyInsight --dependency jackson-databind
```

## Scopes/configurations used here (what each one means)

In `build.gradle` you will see several typical "configurations":

- **`implementation`**: available for compiling and at the app's runtime. It is the normal scope for production dependencies.
- **`compileOnly`**: for compiling only; it is NOT packaged and is not on the runtime classpath. Useful for things like Lombok.
- **`runtimeOnly`**: at runtime only (not needed to compile). E.g. the database/driver.
- **`annotationProcessor`**: annotation processors at compile time (e.g. Lombok).
- **`testImplementation`**: dependencies to compile and run tests.
- **`testRuntimeOnly`**: test runtime only.

This project also has:

- `compileOnly { extendsFrom annotationProcessor }`: makes whatever is in `annotationProcessor` also available for compilation where applicable (very common with Lombok).

## What each declared dependency provides

Summary (based on `build.gradle`):

- **H2 Console** (`org.springframework.boot:spring-boot-h2console`):
  - Enables H2's web console to view tables, run queries and debug data during development.
  - Usually used together with embedded H2; normally exposed under a path like `/h2-console` (depending on configuration).

- **JPA + Spring Data** (`org.springframework.boot:spring-boot-starter-data-jpa`):
  - Brings the persistence stack: Spring Data JPA + a JPA provider (usually Hibernate) + transaction support.
  - Lets you define repositories (interfaces) and entities (`@Entity`) for CRUD, derived queries and pagination.

- **Web MVC** (`org.springframework.boot:spring-boot-starter-webmvc`):
  - Enables the Servlet / Spring MVC stack (controllers, request mapping, binding, converters, etc.) for HTTP APIs.
  - Integrates with the embedded server configured by Boot and with JSON serialization (through transitive dependencies).

- **Validation** (`org.springframework.boot:spring-boot-starter-validation`):
  - Adds Bean Validation (Jakarta Validation + its typical implementation: Hibernate Validator) to validate DTOs and parameters.
  - Enables annotations such as `@NotNull`, `@Size`, `@Email` and their integration with Spring MVC (`@Valid` / `@Validated`).

- **Retry** (`org.springframework.retry:spring-retry`):
  - Provides declarative and imperative retries for operations prone to transient failures (timeouts, locks, etc.).
  - Lets you use `@Retryable` / `@Recover` or programmatic APIs to control backoff, maximum attempts and exceptions.

- **Aspects (AOP)** (`org.springframework:spring-aspects`):
  - Provides the AOP infrastructure needed for annotations such as `@Retryable` to work through proxies/aspects.
  - Without it, `@Retryable` commonly does not apply (because the method is not woven/intercepted at runtime).

- **Lombok** (`org.projectlombok:lombok`) (in `compileOnly` + `annotationProcessor`):
  - Generates code at compile time (getters/setters, constructors, builders, `equals/hashCode`, etc.) from annotations.
  - It is not included in the final runtime; it requires the IDE to have the annotation processing plugin/support enabled.

- **H2 Database** (`com.h2database:h2`) (in `runtimeOnly`):
  - An embedded in-memory or file-based database, widely used for development, demos and quick tests.
  - Loaded at runtime so the datasource can connect; it is usually not needed to compile the project.

- **Test JPA** (`org.springframework.boot:spring-boot-starter-data-jpa-test`) (in `testImplementation`):
  - Utilities and auto-configuration aimed at persistence tests (repositories/entities), reducing the loaded context.
  - It usually brings test tools (including JUnit/Jupiter and assertions) aligned with Spring Boot.

- **Test Web MVC** (`org.springframework.boot:spring-boot-starter-webmvc-test`) (in `testImplementation`):
  - Makes web-layer (controller) tests easier with tools such as `MockMvc` and configuration slices.
  - Lets you validate mappings, serialization, HTTP codes and validations without necessarily starting the whole app.

- **JUnit Platform Launcher** (`org.junit.platform:junit-platform-launcher`) (in `testRuntimeOnly`):
  - Runtime component to discover/run tests on the JUnit platform (useful for certain runners and integrations).
  - Complements the main test dependencies: it is used when running, not needed to compile tests.

## What does the SDK (JDK) have to do with all this?

In Java, when people say "SDK" they usually mean the **JDK** (Java Development Kit): compiler (`javac`), runtime, tools, etc.

In this project, Gradle pins the "target SDK" through **toolchains**:

- In `build.gradle`:
  - `java.toolchain.languageVersion = 17`

That means:

- The project is meant to **compile with Java 17**.
- Gradle will try to use a **JDK 17** to compile, even if your system has several JDKs installed.
- The IDE (IntelliJ, VS Code, etc.) should also point to **JDK 17** to avoid differences (compilation errors, incompatible bytecode, etc.).

### The ecosystem around the JDK in a Spring/Gradle project

- **Gradle**: the build tool; it resolves dependencies, compiles, runs tests and packages.
- **Gradle Wrapper** (`./gradlew`): pins the Gradle version used by the project (here: **Gradle 9.2.1** via `gradle/wrapper/gradle-wrapper.properties`). That way the whole team builds with the same version without installing Gradle globally.
- **Maven repositories**: the "catalog" of artifacts (JARs). Here `mavenCentral()` is used.
- **Spring Boot BOM / dependency management**: decides compatible versions of the Spring stack and common libraries.
- **Annotation processors** (Lombok): they run at compile time and require correct IDE + Gradle configuration.

## Quick tips to avoid common problems

- **Make sure the IDE uses JDK 17** (Project SDK / Gradle JVM).
- **Always use `./gradlew`** (the wrapper) instead of a globally installed Gradle.
- If a version conflict appears, use `dependencyInsight` to see what brings in what.
