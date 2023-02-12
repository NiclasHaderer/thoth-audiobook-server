import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.8.0"
}

repositories {
    mavenCentral()
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
        jvmTarget = "16"
        apiVersion = "1.8"
        languageVersion = "1.8"
    }
}

dependencies {
    implementation(project(":metadata"))

    implementation("com.sksamuel.hoplite:hoplite-core:2.6.2")
    implementation("com.sksamuel.hoplite:hoplite-json:2.6.2")
    implementation("com.sksamuel.hoplite:hoplite-yaml:2.6.2")
    implementation("com.sksamuel.hoplite:hoplite-toml:2.6.2")
    implementation("com.sksamuel.hoplite:hoplite-hocon:2.6.2")
}
