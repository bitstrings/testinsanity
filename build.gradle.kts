import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.javaToolchain.get())
        vendor = JvmVendorSpec.AZUL
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
        ides {
            create(IntelliJPlatformType.IntellijIdeaCommunity, libs.versions.intellijPlatform)
            create(IntelliJPlatformType.IntellijIdea, libs.versions.intellijPlatformNewest)
        }
    }
}

tasks.assemble {
    dependsOn(tasks.buildPlugin)
}

tasks.check {
    dependsOn(tasks.verifyPlugin)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
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

    jvmArgs("-Xshare:off")
}
