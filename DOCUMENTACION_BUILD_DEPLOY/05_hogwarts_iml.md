# `hogwarts.iml`

Archivo cubierto: [`hogwarts.iml`](../hogwarts.iml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<module type="JAVA_MODULE" version="4">
  <component name="NewModuleRootManager" inherit-compiler-output="true">
    <exclude-output />
    <content url="file://$MODULE_DIR$" />
    <orderEntry type="sourceFolder" forTests="false" />
  </component>
</module>
```

## Qué es

Un archivo `.iml` es metadata propia de **IntelliJ IDEA** (y derivados como Android Studio): describe un "módulo" del IDE — su carpeta raíz, si el output del compilador se hereda del proyecto, qué carpetas excluir de la indexación, etc. Este `hogwarts.iml` corresponde al módulo raíz del proyecto y es autogenerado por IntelliJ al importar/abrir la carpeta `hogwarts-backend`.

Desglose de su contenido:
- `type="JAVA_MODULE"`: le indica al IDE que trate esta carpeta como un módulo Java.
- `inherit-compiler-output="true"`: el directorio de salida del compilador (dentro del IDE) se hereda de la configuración global del proyecto, en vez de definirse por módulo.
- `<exclude-output />`: excluye la carpeta de output (equivalente a `build/`) de la indexación del IDE.
- `<content url="file://$MODULE_DIR$" />`: el contenido del módulo es la propia carpeta donde vive el `.iml`.
- `<orderEntry type="sourceFolder" forTests="false" />`: entrada de classpath estándar para las carpetas fuente.

## Qué NO es

**No participa en absoluto del build real.** Gradle no lee `.iml`; Docker no lo copia; Cloud Build/Buildpacks lo ignoran. Es puramente configuración del editor. Si se borra, IntelliJ simplemente lo vuelve a generar la próxima vez que se abre o sincroniza el proyecto (usualmente a partir de la información de `settings.gradle` y los `build.gradle`).

## Nota sobre `.gitignore`

El `.gitignore` raíz incluye una sección estándar de IntelliJ que ignora `*.iml`:

```gitignore
### IntelliJ IDEA ###
.idea
*.iws
*.iml
*.ipr
```

Es decir, la intención declarada en `.gitignore` es que los `.iml` **no** se versionen (son artefactos locales del IDE, propensos a diferir entre máquinas). La presencia de `hogwarts.iml` en el árbol de trabajo no contradice esto de forma problemática — es simplemente un archivo generado localmente por IntelliJ que convive con el resto del código; no es necesario ni recomendable tratarlo como parte del proceso de build/deploy documentado aquí.

## Rol en el pipeline de build/deploy

Ninguno. Se documenta por completitud (fue listado explícitamente como archivo a describir), pero no interviene ni en la compilación local, ni en `./gradlew check`, ni en la construcción de imágenes (Dockerfile o Buildpacks), ni en el despliegue a GKE.
