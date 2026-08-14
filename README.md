# TestInsanity for IDEA
<img alt="TestInsanity Logo" src="resources/META-INF/pluginIconDoc.png" />
<br/>

TestInsanity is an IDEA plugin for renaming tests methods and classes on refactoring. 

# License
Apache License, Version 2.0

[LICENSE.txt](LICENSE.txt)

# Building

Gradle with the IntelliJ Platform Gradle Plugin. Nothing to install and nothing to configure — the platform,
the JDK and every dependency are resolved by the build:

```sh
./gradlew build          # compile, and run every test
./gradlew test           # tests only
./gradlew verifyPlugin   # IntelliJ Plugin Verifier at both ends of the supported IDE range
./gradlew buildPlugin    # the installable zip, in build/distributions
./gradlew runIde         # a sandbox IDE with the plugin loaded
```

Every version lives in `gradle/libs.versions.toml`: the platform the plugin compiles against, the oldest and
newest IDE it is verified on, the `since-build` it declares, the Java toolchain, and the test dependencies.
The compile target is the *oldest* supported IDE, so the compiler rejects any API that IDE does not have.

`test/` holds the suite: JUnit tests for the pattern engine, the scheme model, the settings, the project
configuration parser and writer and the bundled resources, plus platform fixture tests that boot an
in-memory IDE to exercise renaming, navigation, the annotation check and both intentions against real PSI.

# Project configuration

Settings can live in version control instead of in `.idea`. Create `.testinsanity.json` in the project
root:

```json
{
  "schemes": [
    {
      "name": "unit",
      "testClass": "${className}Test",
      "testMethods": ["(test|)${subjectName}*"]
    },
    {
      "name": "it",
      "testClass": "${className}IT",
      "testMethods": ["${subjectName}_+"]
    }
  ],
  "capitalizeSubject": "ifPrefixed",
  "testAnnotations": ["junit4", "junit5", "testng"],
  "additionalTestAnnotations": ["com.acme.testing.AcmeTest", "com.acme.testing.*"],
  "includeInheritedMethods": true,
  "includeInterfacesAndAbstracts": false,
  "includeNestedClasses": true,
  "syncDisplayName": false,
  "refactoring": true,
  "navigation": true,
  "gutterIcons": true,
  "preselectRenames": true
}
```

A scheme binds one test class convention to the test method conventions used inside it, so unit,
integration and end to end tests each keep their own method naming. Schemes are ordered: they are tried
top to bottom and the first whose `testClass` matches wins, so put the more specific one first. Inside a
scheme, `testMethods` is ordered the same way. Settings -> Tools -> TestInsanity edits the list, with add,
remove and reorder, and refuses an invalid method pattern as you type it.

The older flat `testClassPatterns` and `testMethodPatterns` keys are still read, and every method pattern
then applies to every class pattern. They are ignored when `schemes` is present.

Every key is optional, and precedence works like `.editorconfig`: a key the file declares governs the
project and is shown disabled in Settings -> Tools -> TestInsanity; a key the file omits stays an
individual preference. The file is re-read whenever it changes, so switching branches switches scheme.

The file is bound to a JSON schema shipped with the plugin, so keys, allowed values and their
documentation complete as you type and a typo is flagged immediately.

**Settings -> Tools -> TestInsanity** reports whether the file is in use, and its `Use .testinsanity.json
from the project root` checkbox turns the whole mechanism off for your IDE without touching the file — the
settings it governs become editable again immediately. When no file exists yet, the link in that section
creates one from the settings you already have, so you can configure in the dialog and then commit the
result. That checkbox is deliberately not a key in the file: a repository cannot force itself on you.

`capitalizeSubject` is one of `unchanged`, `ifPrefixed`, `always`. `testAnnotations` accepts `junit4`,
`junit5` and `testng`. Patterns are validated on load; an invalid pattern, a malformed file or an
unexpected value is reported in a notification and leaves the previous settings in force. An unknown key
is reported as a warning and ignored, so a file written for a newer plugin version still works.

# Test annotation check

A method counts as a test when it carries one of the annotations of a selected framework. An annotation
**composed** of one of those counts as well, so a project-local

```java
@Test
@Tag("integration")
public @interface IntegrationTest {}
```

is recognized without being declared anywhere, and so is an override of an annotated method. Every
JUnit Platform engine is covered through `org.junit.platform.commons.annotation.Testable`, which
`@TestFactory`, `@TestTemplate`, `@ParameterizedTest`, `@RepeatedTest`, jqwik's `@Property` and
ArchUnit's `@ArchTest` all carry.

`additionalTestAnnotations` adds annotations the plugin does not know about — a house runner, or a
framework it has never heard of. Each entry is a fully qualified annotation name, or a package wildcard
ending in `.*` which matches every annotation in that package and below. Composition applies to a
declared name; a package wildcard is matched on the annotation written on the method itself.

Frameworks that mark tests by DSL or by naming rather than by annotation — Spock, Kotest, ScalaTest,
JUnit 3 — have nothing to declare here: leave the framework boxes unchecked and let the name patterns
do the matching.
