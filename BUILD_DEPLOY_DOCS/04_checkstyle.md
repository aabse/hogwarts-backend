# Checkstyle: `checkstyle.xml` y `suppressions.xml`

Archivos cubiertos:

- [`config/checkstyle/checkstyle.xml`](../config/checkstyle/checkstyle.xml)
- [`config/checkstyle/suppressions.xml`](../config/checkstyle/suppressions.xml)

Se documentan juntos porque están conectados directamente: `checkstyle.xml` declara un `SuppressionFilter` que lee `suppressions.xml`, y ambos son referenciados desde el bloque `checkstyle { ... }` de [`build.gradle`](02_root_build_gradle.md).

## Cómo se conectan con Gradle

```groovy
checkstyle {
    toolVersion = libs.versions.checkstyle.get()   // 12.3.1, en gradle/libs.versions.toml
    configFile = rootProject.file('config/checkstyle/checkstyle.xml')
    configProperties = [
            'checkstyle.suppressions.file': rootProject.file('config/checkstyle/suppressions.xml').absolutePath
    ]
}
```

Gradle inyecta la ruta absoluta de `suppressions.xml` como la propiedad `checkstyle.suppressions.file`, que `checkstyle.xml` consume así:

```xml
<module name="SuppressionFilter">
    <property name="file" value="${checkstyle.suppressions.file}"/>
</module>
```

Esta indirección permite mover `suppressions.xml` de lugar sin tener que hardcodear una ruta dentro de `checkstyle.xml`.

## `checkstyle.xml`

```xml
<module name="Checker">
    <property name="charset" value="UTF-8"/>
    <property name="severity" value="error"/>

    <module name="SuppressionFilter">
        <property name="file" value="${checkstyle.suppressions.file}"/>
    </module>

    <!-- Require package-info.java and package-level JavaDoc -->
    <module name="JavadocPackage"/>

    <module name="TreeWalker">
        <module name="JavadocType">
            <property name="scope" value="private"/>
        </module>

        <module name="JavadocMethod">
            <property name="accessModifiers" value="public,protected,package,private"/>
            <property name="allowMissingParamTags" value="false"/>
            <property name="allowMissingReturnTag" value="false"/>
            <property name="validateThrows" value="true"/>
        </module>

        <module name="JavadocVariable">
            <property name="accessModifiers" value="public,protected,package,private"/>
        </module>

        <module name="UnusedImports"/>
    </module>

    <module name="LineLength">
        <property name="max" value="200"/>
        <property name="ignorePattern" value="^package.*|^import.*|https?://|ftp://"/>
    </module>
</module>
```

Reglas activas, todas con `severity=error` (cualquier violación **rompe el build**, no es solo un warning):

| Módulo | Alcance | Qué exige |
|---|---|---|
| `JavadocPackage` | nivel `Checker` | `package-info.java` con Javadoc en cada paquete |
| `JavadocType` | `scope=private` | Javadoc en toda declaración de tipo (clases, interfaces...), incluso las `private` |
| `JavadocMethod` | todos los modificadores de acceso | Javadoc en todo método, con `@param`/`@return`/`@throws` obligatorios cuando aplican |
| `JavadocVariable` | todos los modificadores de acceso | Javadoc en todo campo/variable, incluso `private` |
| `UnusedImports` | — | prohíbe imports no usados |
| `LineLength` | nivel `Checker` | máximo 200 caracteres por línea (exceptuando `package`, `import` y URLs) |

Es una configuración estricta: exige documentación Javadoc exhaustiva incluso en miembros privados, algo poco común en configuraciones por defecto de Checkstyle.

## `suppressions.xml`

```xml
<suppressions>
    <!-- No suppressions by default. Add narrowly scoped entries only when justified. -->
</suppressions>
```

Actualmente **no define ninguna supresión** — es un archivo "andamio" (scaffold) vacío, listo para agregar excepciones puntuales y justificadas si en el futuro hiciera falta (por ejemplo, excluir una clase generada automáticamente de la regla de Javadoc). El comentario dentro del archivo deja explícita la convención del equipo: agregar supresiones solo cuando estén acotadas y justificadas.

## Rol en el pipeline de build/deploy

Checkstyle corre como parte de `./gradlew check` (tareas `checkstyleMain`/`checkstyleTest`, generadas automáticamente por el plugin `checkstyle` aplicado en `subprojects`). Es una de las verificaciones que se **saltan intencionalmente** al construir la imagen de despliegue para acelerar el build de la imagen — por ejemplo, en [`wizard-service/Dockerfile`](07_wizard_service.md):

```dockerfile
RUN ./gradlew :wizard-service:bootJar -x test -x checkstyleMain -x checkstyleTest -x spotlessCheck --no-daemon
```

Esto asume que el código ya pasó `./gradlew check` (localmente o en un pipeline de CI) **antes** de intentar empaquetarlo — ver el orden completo del pipeline en [`README.md`](README.md).
