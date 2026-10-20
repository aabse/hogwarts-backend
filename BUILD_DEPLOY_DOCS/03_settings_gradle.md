# `settings.gradle`

Archivo cubierto: [`settings.gradle`](../settings.gradle)

```groovy
rootProject.name = 'hogwarts-backend'

include 'wizard-service'
include 'inventory-service'
include 'duel-service'
```

## Qué hace

`settings.gradle` es el **punto de entrada** de un build de Gradle: se evalúa antes que cualquier `build.gradle`, y es donde se define la topología del proyecto multi-módulo.

- `rootProject.name = 'hogwarts-backend'`: nombre lógico del proyecto raíz (usado, por ejemplo, en el nombre del artefacto si se empaquetara la raíz, y en mensajes de Gradle).
- `include 'wizard-service' / 'inventory-service' / 'duel-service'`: declara los tres subproyectos. Cada `include` le dice a Gradle que busque una carpeta con ese nombre en la raíz y que espere encontrar allí un `build.gradle` propio (ver [`07_wizard_service.md`](07_wizard_service.md), [`08_inventory_service.md`](08_inventory_service.md), [`09_duel_service.md`](09_duel_service.md)).

Sin este archivo, `./gradlew :wizard-service:bootJar` (o cualquier tarea con notación de proyecto, `:modulo:tarea`) no existiría — Gradle no sabría que `wizard-service` es un módulo del build.

## Relación con el resto del build

- El bloque `subprojects { ... }` de [`build.gradle`](02_root_build_gradle.md) (Checkstyle, Spotless, JaCoCo, Java 17) se aplica exactamente a los módulos listados aquí.
- La notación `:modulo:tarea` que aparece en:
  - el [`Dockerfile` de `wizard-service`](07_wizard_service.md) (`./gradlew :wizard-service:bootJar -x test ...`),
  - el [`README.md` raíz](../README.md) para correr cada servicio localmente (`./gradlew :inventory-service:bootRun --args="--server.port=8081"`),
  
  solo funciona porque `settings.gradle` incluyó esos módulos.
- Si en el futuro se agregara un cuarto microservicio, el primer paso sería añadir su `include` aquí.

## Rol en el pipeline de build/deploy

Es un archivo pequeño pero crítico: define qué módulos existen y, por lo tanto, qué imágenes de contenedor son "buildeables" (una por módulo). Tanto el flujo por **Dockerfile** (que necesita copiar `settings.gradle` a la imagen porque compila desde la raíz, ver [`README.md`](README.md)) como el flujo por **Buildpacks** (que se ejecuta desde dentro de cada subcarpeta, ver `GKE_GCP_DEPLOYMENT/03_artifact_registry_and_builds.md`) dependen de esta topología para saber qué compilar de forma aislada.
