# Checkstyle: `checkstyle.xml` and `suppressions.xml`

Files covered:

- [`config/checkstyle/checkstyle.xml`](../config/checkstyle/checkstyle.xml)
- [`config/checkstyle/suppressions.xml`](../config/checkstyle/suppressions.xml)

They are documented together because they are directly connected: `checkstyle.xml` declares a `SuppressionFilter` that reads `suppressions.xml`, and both are referenced from the `checkstyle { ... }` block of [`build.gradle`](02_root_build_gradle.md).

## How they connect to Gradle

```groovy
checkstyle {
    toolVersion = libs.versions.checkstyle.get()   // 12.3.1, in gradle/libs.versions.toml
    configFile = rootProject.file('config/checkstyle/checkstyle.xml')
    configProperties = [
            'checkstyle.suppressions.file': rootProject.file('config/checkstyle/suppressions.xml').absolutePath
    ]
}
```

Gradle injects the absolute path of `suppressions.xml` as the `checkstyle.suppressions.file` property, which `checkstyle.xml` consumes like this:

```xml
<module name="SuppressionFilter">
    <property name="file" value="${checkstyle.suppressions.file}"/>
</module>
```

This indirection lets you move `suppressions.xml` without hardcoding a path inside `checkstyle.xml`.

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

Active rules, all with `severity=error` (any violation **breaks the build**; it is not just a warning):

| Module | Scope | What it requires |
|---|---|---|
| `JavadocPackage` | `Checker` level | A `package-info.java` with Javadoc in every package |
| `JavadocType` | `scope=private` | Javadoc on every type declaration (classes, interfaces...), even `private` ones |
| `JavadocMethod` | all access modifiers | Javadoc on every method, with `@param`/`@return`/`@throws` required where applicable |
| `JavadocVariable` | all access modifiers | Javadoc on every field/variable, even `private` ones |
| `UnusedImports` | — | forbids unused imports |
| `LineLength` | `Checker` level | at most 200 characters per line (except `package`, `import` and URLs) |

It is a strict configuration: it requires exhaustive Javadoc documentation even on private members, which is unusual in Checkstyle's default configurations.

## `suppressions.xml`

```xml
<suppressions>
    <!-- No suppressions by default. Add narrowly scoped entries only when justified. -->
</suppressions>
```

It currently **defines no suppressions** — it is an empty "scaffold" file, ready for adding specific, justified exceptions if they are ever needed (for example, excluding an auto-generated class from the Javadoc rule). The comment inside the file makes the team's convention explicit: add suppressions only when they are narrowly scoped and justified.

## Role in the build/deploy pipeline

Checkstyle runs as part of `./gradlew check` (the `checkstyleMain`/`checkstyleTest` tasks, generated automatically by the `checkstyle` plugin applied in `subprojects`). It is one of the checks that are **intentionally skipped** when building the deployment image, to speed up the image build — for example, in [`wizard-service/Dockerfile`](07_wizard_service.md):

```dockerfile
RUN ./gradlew :wizard-service:bootJar -x test -x checkstyleMain -x checkstyleTest -x spotlessCheck --no-daemon
```

This assumes the code has already passed `./gradlew check` (locally or in a CI pipeline) **before** trying to package it — see the full pipeline order in [`README.md`](README.md).
