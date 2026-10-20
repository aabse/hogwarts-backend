# Directorios generados: `/build` y `/.gradle`

Carpetas cubiertas:

- [`/build`](../build)
- [`/.gradle`](../.gradle)

Se documentan juntas porque ambas son **estado generado** por el proceso de build de Gradle (no código fuente), ambas están excluidas de git, y ambas se recrean automáticamente al compilar — nunca deben editarse ni commitearse a mano.

## `/build` (raíz)

Es la salida del build del **proyecto raíz** (cada subproyecto tiene su propio `build/` interno, p. ej. `wizard-service/build/`). Contenido típico:

```
build/
├── classes/         # clases .class compiladas
├── generated/        # fuentes/recursos generados por plugins
├── reports/          # reportes de jacoco, checkstyle, tests, etc.
│   └── jacoco/jacocoRootReport/{jacocoRootReport.xml,html/}
├── resources/         # recursos procesados
├── test-results/       # resultados de test (XML, usados por CI para reportar)
└── tmp/             # archivos temporales de tareas de Gradle
```

Es aquí donde aterrizan, entre otras cosas, las salidas de las tareas custom definidas en [`build.gradle`](02_root_build_gradle.md): `jacocoRootReport` escribe explícitamente en `build/reports/jacoco/jacocoRootReport/`. En cada subproyecto, `build/libs/*.jar` es el artefacto que produce `bootJar` y que consume el `Dockerfile` de `wizard-service` (`COPY --from=build /workspace/wizard-service/build/libs/*.jar app.jar`, ver [`07_wizard_service.md`](07_wizard_service.md)).

## `/.gradle` (raíz)

Es la **caché local del proyecto** (distinta de `~/.gradle`, el `GRADLE_USER_HOME` global mencionado en [`01_gradle_wrapper.md`](01_gradle_wrapper.md)). Contenido típico:

```
.gradle/
├── <version>/          # metadata/caché interna de Gradle para esa versión (p. ej. 9.2.1)
├── buildOutputCleanup/  # estado del servicio de limpieza de outputs de Gradle
├── file-system.probe    # detección de sensibilidad a mayúsculas/granularidad de timestamps del filesystem
└── vcs-1/               # caché de mapeos VCS (para builds incluidos/dependencias por fuente)
```

Permite builds incrementales más rápidos (Gradle evita recompilar/re-testear lo que no cambió) pero es puramente un artefacto de la máquina local — no debe compartirse entre entornos ni copiarse a una imagen de contenedor.

## Por qué están (correctamente) fuera de git

`.gitignore` raíz:

```gitignore
.gradle
build/
!gradle/wrapper/gradle-wrapper.jar
!**/src/main/**/build/
!**/src/test/**/build/
```

- `.gradle` y `build/` se ignoran en cualquier nivel del árbol (afecta tanto a la raíz como a `wizard-service/build/`, `inventory-service/build/`, `duel-service/build/`).
- Las dos excepciones (`!**/src/main/**/build/`, `!**/src/test/**/build/`) son un guardado defensivo poco común: protegen cualquier archivo que, por coincidencia, viva dentro de una carpeta llamada literalmente `build` dentro de un árbol fuente (`src/main/.../build/...`), distinguiéndola del directorio de salida del build en la raíz de cada módulo.
- La excepción de `gradle-wrapper.jar` (ver [`01_gradle_wrapper.md`](01_gradle_wrapper.md)) es necesaria porque ese binario sí debe commitearse.

## Rol en el pipeline de build/deploy

Estos directorios son **transitorios y reproducibles**: cualquier entorno (una laptop, un runner de CI, la etapa de build de un `Dockerfile`, o Cloud Build ejecutando Buildpacks) los regenera desde cero a partir del código fuente y de `build.gradle`/`settings.gradle`. Por eso el `Dockerfile` de `wizard-service` nunca copia `build/` ni `.gradle/` al contenedor — copia el código fuente y ejecuta `./gradlew ... bootJar` dentro de la imagen, dejando que Gradle regenere `build/libs/*.jar` en ese entorno aislado.
