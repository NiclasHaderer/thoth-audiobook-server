val tsGeneratorVersion: String by project
val kotlinVersion: String by project
plugins {
    kotlin("jvm") version "1.8.0"
}

repositories {
    maven("https://jitpack.io")
    mavenCentral()
}

dependencies {
    implementation(project(":metadata"))
    implementation(project(":models"))
    implementation(project(":server"))
    // Typescript code generation
    implementation("com.github.ntrrgc:ts-generator:$tsGeneratorVersion")
}
