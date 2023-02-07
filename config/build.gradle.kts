plugins {
    id("java")
    kotlin("jvm") version "1.8.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":metadata"))

    implementation("com.sksamuel.hoplite:hoplite-core:2.6.2")
    implementation("com.sksamuel.hoplite:hoplite-json:2.6.2")
    implementation("com.sksamuel.hoplite:hoplite-yaml:2.6.2")
    implementation("com.sksamuel.hoplite:hoplite-toml:2.6.2")
    implementation("com.sksamuel.hoplite:hoplite-hocon:2.6.2")
}
