# `build.gradle` (raíz)

Archivos cubiertos:

- [`build.gradle`](../build.gradle)
- [`gradle/libs.versions.toml`](../gradle/libs.versions.toml)

Es el archivo de configuración compartida de Gradle: define lo que aplica al proyecto raíz y, mediante el bloque `subprojects { ... }`, lo que se aplica automáticamente a los tres microservicios (`wizard-service`, `inventory-service`, `duel-service`). No declara el plugin de Spring Boot — eso vive en el `build.gradle` de cada servicio (ver [`07_wizard_service.md`](07_wizard_service.md), [`08_inventory_service.md`](08_inventory_service.md), [`09_duel_service.md`](09_duel_service.md)), porque el proyecto raíz en sí no es una aplicación ejecutable, solo el "contenedor" de configuración común.

## Plugins y metadata del proyecto raíz

```groovy
plugins {
    id 'java'
    id 'jacoco'
    alias(libs.plugins.spotless) apply false
}

group = 'com.magic'
version = '0.0.1-SNAPSHOT'
description = 'hogwarts'
```

- `java` y `jacoco` se aplican también al proyecto raíz.
- `spotless` se declara con `apply false`: su versión sale del catálogo de versiones (ver la sección siguiente), pero solo se **activa** dentro de `subprojects`, no en la raíz.
- `group` (`com.magic`) lo heredan siempre los tres servicios.
- `version` (`0.0.1-SNAPSHOT`) es solo el **valor por defecto**: cada servicio declara su propia versión en su `gradle.properties` (ver la sección [Versión de cada servicio](#versión-de-cada-servicio)).

## Catálogo de versiones: `gradle/libs.versions.toml`

```toml
[versions]
spotless = "8.1.0"
jacoco = "0.8.12"
checkstyle = "12.3.1"

[plugins]
spotless = { id = "com.diffplug.spotless", version.ref = "spotless" }
```

Gradle carga este archivo automáticamente (por estar en `gradle/` con ese nombre) y lo expone en los scripts como `libs`. Centraliza en un solo lugar las versiones de las **herramientas de build del root**:

| Herramienta | Entrada del catálogo | Dónde se usa en `build.gradle` |
|---|---|---|
| Spotless | `libs.plugins.spotless` | Bloque `plugins` |
| JaCoCo | `libs.versions.jacoco.get()` | `jacoco { toolVersion = ... }`, en la raíz y en `subprojects` |
| Checkstyle | `libs.versions.checkstyle.get()` | `checkstyle { toolVersion = ... }`, en `subprojects` |

Para actualizar una de estas herramientas, se cambia solo su línea en `[versions]`.

**Por qué Spring Boot y las dependencias de los servicios no están en el catálogo**: el build con Buildpacks (ver [`README.md`](README.md) §4.a) se ejecuta desde la carpeta de cada servicio y Cloud Build sube **solo esa carpeta**. Ahí no existe `gradle/libs.versions.toml`, así que un `build.gradle` de servicio que usara `libs.…` no compilaría. Por eso cada servicio declara sus propias versiones (Spring Boot, `spring-retry`, conector de Cloud SQL) y su bloque `repositories`. El costo es que la versión de Spring Boot se repite en los tres `build.gradle` de servicio: al actualizarla, hay que cambiarla en los tres.

## Versión de cada servicio

Cada servicio se despliega y se versiona por separado, así que su versión vive en un `gradle.properties` dentro de su carpeta:

```properties
# wizard-service/gradle.properties
# Version propia del servicio (se usa en el jar y en los tags de imagen).
version=0.0.1-SNAPSHOT
```

Gradle lee ese archivo al crear el subproyecto. Para que la raíz no lo pise, el bloque `subprojects` solo asigna la versión del root a los servicios que no tienen una propia:

```groovy
subprojects {
    group = rootProject.group
    // Cada servicio define su version en su gradle.properties; si no la define, hereda la del root.
    if (version.toString() == 'unspecified') {
        version = rootProject.version
    }
    ...
}
```

Esa versión da nombre al jar (`wizard-service-0.0.1-SNAPSHOT.jar`) y es también el tag de la imagen de contenedor. Las reglas para incrementarla (versionado semántico `MAJOR.MINOR.PATCH`) y el flujo de release están en [`../DESPLIEGUE_GKE_GCP/03_artifact_registry_y_builds.md`](../DESPLIEGUE_GKE_GCP/03_artifact_registry_y_builds.md) §4.

Como `gradle.properties` está dentro de la carpeta del servicio, también funciona en el build aislado de Buildpacks.

Para ver la versión resuelta de un servicio:

```bash
./gradlew -q :wizard-service:properties | grep '^version:'
```

## Toolchain de Java

```groovy
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
```

Fija **Java 17** tanto en la raíz como (repetido) dentro de `subprojects`. Gradle usa esta toolchain para compilar y ejecutar, independientemente del JDK que tenga instalado quien ejecuta el build (siempre que Gradle pueda localizar o descargar un JDK 17).

## Bloque `subprojects { ... }`

Se aplica automáticamente a `wizard-service`, `inventory-service` y `duel-service` (los módulos declarados en [`settings.gradle`](03_settings_gradle.md)):

- Aplica los plugins `java`, `checkstyle`, `com.diffplug.spotless`, `jacoco`.
- Asigna `group` y, si el servicio no tiene una propia, `version` (ver [Versión de cada servicio](#versión-de-cada-servicio)).
- Repite el toolchain de Java 17 y el repositorio `mavenCentral()`.
- **Checkstyle**:
  ```groovy
  checkstyle {
      toolVersion = libs.versions.checkstyle.get()
      configFile = rootProject.file('config/checkstyle/checkstyle.xml')
      configProperties = [
              'checkstyle.suppressions.file': rootProject.file('config/checkstyle/suppressions.xml').absolutePath
      ]
  }
  ```
  Ver el detalle completo de las reglas en [`04_checkstyle.md`](04_checkstyle.md). Los reportes de Checkstyle se fuerzan en XML y HTML (`tasks.withType(Checkstyle).configureEach { reports { xml.required = true; html.required = true } }`).
- **Spotless** (formateo automático de Java):
  ```groovy
  spotless {
      java {
          googleJavaFormat()
          formatAnnotations()
          trimTrailingWhitespace()
          endWithNewline()
      }
  }
  ```
  Usa Google Java Format. `tasks.named('check') { dependsOn 'spotlessCheck' }` hace que un formateo incorrecto **rompa** `./gradlew check`.
- **JaCoCo por módulo**: `toolVersion = libs.versions.jacoco.get()` (0.8.12); `test` queda `finalizedBy 'jacocoTestReport'` (el reporte de cobertura se genera automáticamente después de cada `test`), con reportes XML y HTML forzados.

## Tareas personalizadas a nivel raíz

```groovy
tasks.register('testAll') {
    dependsOn(subprojects.collect { it.tasks.matching { t -> t.name == 'test' } })
}
```
Ejecuta los tests de los tres módulos con un solo comando (`./gradlew testAll`), sin correr el resto del quality gate (spotless/checkstyle/cobertura).

```groovy
def coverageExclusions = [
        '**/*Application.*',
        '**/*DTO.*',
        '**/common/**',
        '**/domain/model/**',
        '**/infrastructure/persistence/**',
        '**/package-info.*'
]

tasks.register('jacocoRootReport', JacocoReport) { ... }
tasks.register('jacocoRootCoverageVerification', JacocoCoverageVerification) { ... }
```

- `jacocoRootReport`: agrega los `.exec` de cobertura de los tres módulos en un único reporte (`build/reports/jacoco/jacocoRootReport/{jacocoRootReport.xml,html}`), excluyendo de la métrica las clases `*Application`, DTOs, código `common`, modelos de dominio, la capa de persistencia y `package-info`.
- `jacocoRootCoverageVerification`: aplica un **gate de cobertura mínima del 90% en líneas** (`counter = 'LINE'`, `value = 'COVEREDRATIO'`, `minimum = 0.90`) sobre ese reporte agregado.
- Finalmente:
  ```groovy
  tasks.named('check') {
      dependsOn 'jacocoRootCoverageVerification'
  }
  ```
  Esto encadena todo: `./gradlew check` en la raíz dispara compilación + tests de los tres módulos + `spotlessCheck` + Checkstyle + el gate de cobertura del 90%. Es el comando que valida que el proyecto está listo para compilar/empaquetar antes de generar cualquier imagen de despliegue.

## Rol en el pipeline de build/deploy

Este es el archivo que Gradle lee primero (junto con `settings.gradle` y el catálogo `gradle/libs.versions.toml`) para saber qué reglas de calidad y qué versiones de herramientas aplican a **todo** el monorepo. Los `Dockerfile` de los servicios compilan desde la raíz, así que usan esta configuración, aunque saltan los pasos de calidad (`-x test -x checkstyleMain -x spotlessCheck`) para acelerar la imagen. El build vía Buildpacks, en cambio, compila cada servicio aislado y **no** usa este archivo. En los dos casos, lo que garantiza que el código cumple estas reglas es `./gradlew check`, ejecutado antes (en local o en CI) de construir la imagen. Ver el flujo completo en [`README.md`](README.md).
