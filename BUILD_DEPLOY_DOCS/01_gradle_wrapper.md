# Gradle Wrapper

Archivos cubiertos:

- [`gradlew`](../gradlew)
- [`gradlew.bat`](../gradlew.bat)
- [`gradle/wrapper/gradle-wrapper.jar`](../gradle/wrapper/gradle-wrapper.jar)
- [`gradle/wrapper/gradle-wrapper.properties`](../gradle/wrapper/gradle-wrapper.properties)

Los cuatro archivos forman un único mecanismo — el **Gradle Wrapper** — por lo que se documentan juntos.

## Qué problema resuelve

El Wrapper permite compilar el proyecto sin tener Gradle instalado globalmente en la máquina, y garantiza que **todos** (desarrolladores, CI, Docker) usan exactamente la misma versión de Gradle. Es el primer eslabón de cualquier flujo de build de este repo: todo comando (`./gradlew check`, `./gradlew :wizard-service:bootJar`, etc.) pasa por aquí antes de llegar a `settings.gradle` o a los `build.gradle`.

## `gradle-wrapper.properties`

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-9.2.1-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

- **Versión fijada: Gradle 9.2.1**, distribución `-bin` (solo binarios, sin fuentes/documentación — descarga más liviana que `-all`).
- `distributionBase` / `zipStoreBase = GRADLE_USER_HOME`: la distribución descargada se cachea en `~/.gradle/wrapper/dists`, **no** dentro del repo. Por eso una máquina nueva tarda un poco más la primera vez que corre `./gradlew` (descarga ~100 MB), pero builds posteriores reutilizan la caché.
- `networkTimeout=10000`: 10 segundos de timeout de red al descargar la distribución.
- `validateDistributionUrl=true`: valida la integridad/autenticidad de la distribución descargada (hardening de seguridad de versiones recientes del wrapper).

Este archivo es la **única** fuente de verdad sobre qué versión de Gradle usa el proyecto. Si se necesita cambiar de versión, se edita solo este archivo (o se corre `./gradlew wrapper --gradle-version=X`).

## `gradlew` / `gradlew.bat`

Son los scripts lanzadores generados por Gradle (no se editan a mano). `gradlew` es la versión para macOS/Linux (shell script) y `gradlew.bat` la versión para Windows (batch). Ambos hacen lo mismo:

1. Resuelven `APP_HOME` (la raíz del proyecto), siguiendo symlinks si hace falta.
2. Localizan un JDK válido (`JAVA_HOME` o `java` en el `PATH`); si no encuentran ninguno, fallan con un mensaje explicativo.
3. Arrancan una JVM liviana (`-Xmx64m -Xms64m`) cuyo único trabajo es ejecutar `gradle-wrapper.jar`.
4. Delegan la ejecución real del build a esa JVM: `java -jar gradle/wrapper/gradle-wrapper.jar <argumentos>`.

## `gradle-wrapper.jar`

Es el binario (~46 KB) que ambos scripts invocan. Su lógica: lee `gradle-wrapper.properties`, descarga (si no está cacheada) la distribución de Gradle 9.2.1 indicada en `distributionUrl`, la valida y finalmente le pasa el control a esa instalación de Gradle para que ejecute la tarea pedida (`check`, `bootJar`, `bootRun`, etc.).

Es un binario y, por convención de Gradle, **debe** commitearse al repositorio para que el wrapper funcione en un clon nuevo sin pasos manuales. Por eso el `.gitignore` raíz lo excluye explícitamente de las reglas que ignoran artefactos de build:

```gitignore
.gradle
build/
!gradle/wrapper/gradle-wrapper.jar
```

## Rol en el pipeline de build/deploy

- **Local**: todo comando de build (`./gradlew check`, `./gradlew testAll`, `./gradlew :wizard-service:bootRun ...`) pasa primero por aquí.
- **Dockerfile**: la etapa de build de [`wizard-service/Dockerfile`](07_wizard_service.md) copia `gradlew`, `gradlew.bat` y la carpeta `gradle/` al contenedor y ejecuta `./gradlew :wizard-service:bootJar` dentro de la imagen — el wrapper es lo que permite compilar sin necesitar una imagen base con Gradle preinstalado.
- **Buildpacks**: Cloud Native Buildpacks para Java/Gradle también detecta y usa `gradlew` automáticamente dentro del directorio del módulo (ver [`README.md`](README.md)).

Ver también: [`02_root_build_gradle.md`](02_root_build_gradle.md), [`03_settings_gradle.md`](03_settings_gradle.md).
