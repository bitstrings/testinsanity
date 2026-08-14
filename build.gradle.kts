import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel

plugins {
    id("java")
    id("org.jetbrains.intellij.platform")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.javaToolchain.get())
    }
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("resources"))
    }

    test {
        java.setSrcDirs(listOf("test"))
        resources.setSrcDirs(emptyList<String>())
    }
}

dependencies {
    intellijPlatform {
        create(IntelliJPlatformType.IntellijIdeaCommunity, libs.versions.intellijPlatform)

        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
        bundledPlugin("com.intellij.modules.json")

        testFramework(TestFrameworkType.Platform)
    }

    compileOnly(libs.errorProneAnnotations)

    testCompileOnly(libs.errorProneAnnotations)

    testImplementation(libs.junit)
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = libs.versions.pluginSinceBuild
            untilBuild = provider { null }
        }
    }

    pluginVerification {
        failureLevel =
            listOf(
                FailureLevel.COMPATIBILITY_PROBLEMS,
                FailureLevel.DEPRECATED_API_USAGES,
                FailureLevel.INTERNAL_API_USAGES,
                FailureLevel.INVALID_PLUGIN,
                FailureLevel.MISSING_DEPENDENCIES,
                FailureLevel.NON_EXTENDABLE_API_USAGES,
                FailureLevel.OVERRIDE_ONLY_API_USAGES,
                FailureLevel.PLUGIN_STRUCTURE_WARNINGS,
                FailureLevel.SCHEDULED_FOR_REMOVAL_API_USAGES,
            )

        ides {
            create(IntelliJPlatformType.IntellijIdeaCommunity, libs.versions.intellijPlatform)
            create(IntelliJPlatformType.IntellijIdea, libs.versions.intellijPlatformNewest)
        }
    }
}

tasks.assemble {
    dependsOn(tasks.buildPlugin)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.buildSearchableOptions {
    val logFile = layout.buildDirectory.file("logs/buildSearchableOptions.log")

    outputs.file(logFile)

    doFirst {
        val log = logFile.get().asFile

        log.parentFile.mkdirs()

        standardOutput = log.outputStream()
        errorOutput = standardOutput
    }
}

tasks.test {
    useJUnit()

    maxHeapSize = "2g"

    jvmArgs("-Xshare:off")

    systemProperty("java.awt.headless", "true")

    testLogging {
        events(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
        exceptionFormat = TestExceptionFormat.FULL
        showStackTraces = true
    }
}
