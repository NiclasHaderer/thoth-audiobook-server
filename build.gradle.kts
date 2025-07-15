import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

val kotlinVersion: String by project

plugins {
    base
    kotlin("jvm") version "2.2.0" apply false
    kotlin("plugin.serialization") version "2.2.0" apply false
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
        compilerOptions {
            freeCompilerArgs.add("-Xopt-in=kotlin.RequiresOptIn")
            jvmTarget.set(JvmTarget.JVM_17)
            apiVersion.set(KotlinVersion.fromVersion(kotlinVersion))
            languageVersion.set(KotlinVersion.fromVersion(kotlinVersion))
            optIn.add("kotlin.RequiresOptIn")
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
