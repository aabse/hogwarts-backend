# Dependencias en este proyecto (Java + Spring Boot + Gradle)

Este proyecto (`hogwarts`) es una aplicación **Spring Boot** construida con **Gradle**. La “historia” de dependencias se define principalmente en `build.gradle` y se resuelve desde repositorios Maven (por defecto **Maven Central**).

## ¿Qué es una dependencia?

Una dependencia es una librería externa que tu código usa para compilar y/o ejecutarse. En Gradle (Java), se declaran con coordenadas Maven:

- **groupId:artifactId:version** (por ejemplo `org.springframework.retry:spring-retry`)

Gradle descarga esas librerías (y sus dependencias “hijas”) y las coloca en el **classpath** de compilación/ejecución/test según el “scope” que elijas.

## ¿Dónde se declaran en este proyecto?

En `build.gradle`:

- **Plugins**:
  - `org.springframework.boot` (`4.0.1`): integra Spring Boot en el build (run, packaging, etc.).
  - `io.spring.dependency-management` (`1.1.7`): aplica **dependency management** (alineación de versiones vía BOM).
  - `java`: soporte estándar Java.
- **Repositorios**:
  - `mavenCentral()`: de aquí se descargan las dependencias.
- **Dependencias**:
  - Se declaran dentro de `dependencies { ... }`.

## Spring Boot, “starters” y versiones (BOM / dependency management)

Spring Boot suele usarse con **starters** (dependencias “paquete” que traen un conjunto coherente de librerías).

En este proyecto hay starters como:

- `org.springframework.boot:spring-boot-starter-data-jpa`
- `org.springframework.boot:spring-boot-starter-webmvc`
- `org.springframework.boot:spring-boot-starter-validation`

Lo importante: en el `build.gradle` **no se especifican versiones** para la mayoría de dependencias de Spring Boot. Eso funciona porque:

- El plugin de Spring Boot y/o `io.spring.dependency-management` aplican un **BOM** (Bill Of Materials) que fija versiones compatibles entre sí.
- Resultado: evitas “version hell” (mezclas incompatibles) y mantienes el ecosistema alineado.

## Dependencias transitivas (el “árbol”)

Cuando agregas un starter, Gradle también trae **dependencias transitivas**. Ejemplo: `spring-boot-starter-webmvc` trae Spring MVC y, típicamente, cosas como Jackson, logging, etc. (dependiendo de la configuración del starter y la versión de Boot).

Puedes inspeccionar el árbol con:

```bash
./gradlew dependencies
```

Y para entender por qué una librería llegó (o qué versión se eligió):

```bash
./gradlew dependencyInsight --dependency jackson-databind
```

## Scopes/configurations usadas aquí (qué significa cada una)

En `build.gradle` verás varias “configurations” típicas:

- **`implementation`**: disponible para compilar y en runtime de la app. Es el scope normal para dependencias de producción.
- **`compileOnly`**: solo para compilar; NO se empaqueta ni va en runtime. Útil para cosas como Lombok.
- **`runtimeOnly`**: solo en ejecución (no es necesaria para compilar). Ej: el driver/base de datos.
- **`annotationProcessor`**: procesadores de anotaciones en compilación (ej. Lombok).
- **`testImplementation`**: dependencias para compilar y correr tests.
- **`testRuntimeOnly`**: solo runtime de tests.

En este proyecto también hay:

- `compileOnly { extendsFrom annotationProcessor }`: hace que lo que esté en `annotationProcessor` también esté disponible para compilación cuando aplique (muy común con Lombok).

## Qué aporta cada dependencia declarada

Resumen (según `build.gradle`):

- **H2 Console** (`org.springframework.boot:spring-boot-h2console`):
  - Habilita la consola web de H2 para ver tablas, ejecutar queries y depurar datos durante desarrollo.
  - Suele usarse junto con H2 embebida; normalmente se expone bajo una ruta tipo `/h2-console` (según configuración).

- **JPA + Spring Data** (`org.springframework.boot:spring-boot-starter-data-jpa`):
  - Trae el stack de persistencia: Spring Data JPA + un proveedor JPA (habitualmente Hibernate) + soporte de transacciones.
  - Permite definir repositorios (interfaces) y entidades (`@Entity`) para CRUD, consultas derivadas y paginación.

- **Web MVC** (`org.springframework.boot:spring-boot-starter-webmvc`):
  - Activa el stack Servlet / Spring MVC (controllers, request mapping, binding, converters, etc.) para APIs HTTP.
  - Se integra con el servidor embebido configurado por Boot y con serialización JSON (vía dependencias transitivas).

- **Validación** (`org.springframework.boot:spring-boot-starter-validation`):
  - Añade Bean Validation (Jakarta Validation + implementación típica: Hibernate Validator) para validar DTOs y parámetros.
  - Habilita anotaciones como `@NotNull`, `@Size`, `@Email` y su integración con Spring MVC (`@Valid` / `@Validated`).

- **Retry** (`org.springframework.retry:spring-retry`):
  - Proporciona reintentos declarativos e imperativos para operaciones propensas a fallos transitorios (timeouts, locks, etc.).
  - Permite usar `@Retryable` / `@Recover` o APIs programáticas para controlar backoff, máximos intentos y excepciones.

- **Aspects (AOP)** (`org.springframework:spring-aspects`):
  - Aporta infraestructura de AOP necesaria para que anotaciones como `@Retryable` funcionen mediante proxies/aspectos.
  - Sin esto, es común que `@Retryable` no se aplique (porque no se teje/intercepta el método en runtime).

- **Lombok** (`org.projectlombok:lombok`) (en `compileOnly` + `annotationProcessor`):
  - Genera código en compilación (getters/setters, constructors, builders, `equals/hashCode`, etc.) a partir de anotaciones.
  - No se incluye en el runtime final; requiere que el IDE tenga el plugin/soporte de annotation processing habilitado.

- **H2 Database** (`com.h2database:h2`) (en `runtimeOnly`):
  - Base de datos embebida en memoria o en archivo, muy usada para desarrollo, demos y tests rápidos.
  - Se carga en runtime para que el datasource pueda conectarse; no suele hacer falta para compilar el proyecto.

- **Test JPA** (`org.springframework.boot:spring-boot-starter-data-jpa-test`) (en `testImplementation`):
  - Utilidades y auto-configuración orientada a tests de persistencia (repositorios/entidades), reduciendo el contexto cargado.
  - Suele traer herramientas de test (incluyendo JUnit/Jupiter y aserciones) alineadas con Spring Boot.

- **Test Web MVC** (`org.springframework.boot:spring-boot-starter-webmvc-test`) (en `testImplementation`):
  - Facilita tests de capa web (controllers) con herramientas como `MockMvc` y slices de configuración.
  - Permite validar mapeos, serialización, códigos HTTP y validaciones sin levantar necesariamente toda la app.

- **JUnit Platform Launcher** (`org.junit.platform:junit-platform-launcher`) (en `testRuntimeOnly`):
  - Componente de runtime para descubrir/ejecutar tests en la plataforma JUnit (útil para ciertos runners e integraciones).
  - Complementa a las dependencias de test principales: se usa al ejecutar, no es necesario para compilar tests.

## ¿Qué tiene que ver el SDK (JDK) con todo esto?

En Java, cuando la gente dice “SDK” normalmente se refiere al **JDK** (Java Development Kit): compilador (`javac`), runtime, herramientas, etc.

En este proyecto, Gradle fija el “SDK objetivo” mediante **toolchains**:

- En `build.gradle`:
  - `java.toolchain.languageVersion = 17`

Eso significa:

- El proyecto está pensado para **compilar con Java 17**.
- Gradle intentará usar un **JDK 17** para compilar, aunque tu sistema tenga varios JDK instalados.
- El IDE (IntelliJ, VS Code, etc.) debe apuntar también a **JDK 17** para evitar diferencias (errores de compilación, bytecode incompatible, etc.).

### Ecosistema alrededor del JDK en un proyecto Spring/Gradle

- **Gradle**: herramienta de build; resuelve dependencias, compila, corre tests y empaqueta.
- **Gradle Wrapper** (`./gradlew`): fija la versión de Gradle usada por el proyecto (aquí: **Gradle 9.2.1** via `gradle/wrapper/gradle-wrapper.properties`). Así todo el equipo construye con la misma versión sin instalar Gradle global.
- **Repositorios Maven**: “catálogo” de artefactos (JARs). Aquí se usa `mavenCentral()`.
- **Spring Boot BOM / dependency management**: decide versiones compatibles del stack Spring y librerías comunes.
- **Annotation processors** (Lombok): se ejecutan en compilación, requieren configuración correcta del IDE + Gradle.

## Consejos rápidos para evitar problemas típicos

- **Asegura JDK 17 en el IDE** (Project SDK / Gradle JVM).
- **Usa siempre `./gradlew`** (wrapper) en lugar de un Gradle instalado globalmente.
- Si aparece un conflicto de versiones, usa `dependencyInsight` para ver quién trae qué.

