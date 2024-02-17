import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    base
    kotlin("jvm") version "1.9.0" apply false
    kotlin("plugin.serialization") version "1.9.0" apply false
    id("com.ncorti.ktfmt.gradle") version "0.13.0"
}

ktfmt { kotlinLangStyle() }

subprojects {
    group = "io.thoth"
    version = "0.0.1"

    repositories {
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
        maven("https://jitpack.io")
    }

    afterEvaluate {
        ktfmt {
            kotlinLangStyle()
            maxWidth.set(120)
            removeUnusedImports.set(true)
        }
    }

    plugins.withType<JavaPlugin> {
        configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
            jvmTarget = "17"
            apiVersion = "1.8"
            languageVersion = "1.8"
        }
    }
}

tasks.getByName("build").dependsOn("installLocalGitHook")

tasks.register<DefaultTask>("installLocalGitHook") {
    val file = File(rootProject.rootDir, ".hooks/pre-commit")
    val target = File(rootProject.rootDir, ".git/hooks/pre-commit")
    if (!file.exists()) {
        throw GradleException("File ${file.absolutePath} does not exist")
    }

    file.copyTo(target, overwrite = true)
    target.setExecutable(true)
}
