val tsGeneratorVersion: String by project

plugins {
    application
    kotlin("jvm") version "1.6.21"
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
