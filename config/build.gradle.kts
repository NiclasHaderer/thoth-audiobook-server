import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val hopliteVersion: String by project

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

    implementation("com.sksamuel.hoplite:hoplite-core:$hopliteVersion")
    implementation("com.sksamuel.hoplite:hoplite-json:$hopliteVersion")
    implementation("com.sksamuel.hoplite:hoplite-yaml:$hopliteVersion")
    implementation("com.sksamuel.hoplite:hoplite-toml:$hopliteVersion")
    implementation("com.sksamuel.hoplite:hoplite-hocon:$hopliteVersion")
}
