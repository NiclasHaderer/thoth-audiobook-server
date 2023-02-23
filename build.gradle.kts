import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    base
    kotlin("jvm") version "1.8.0" apply false
    kotlin("plugin.serialization") version "1.8.0" apply false
}

subprojects {
    group = "io.thoth"
    version = "0.0.1"

    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
            jvmTarget = "16"
            apiVersion = "1.8"
            languageVersion = "1.8"
        }
    }

}