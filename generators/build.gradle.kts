val tsGeneratorVersion: String by project

plugins {
    application
    kotlin("jvm") version "1.5.31"
}

repositories {
    maven("https://jitpack.io")
    mavenCentral()
}

dependencies{
    implementation(project(":server"))
    implementation(project(":metadata"))
    // Typescript code generation
    implementation("com.github.ntrrgc:ts-generator:$tsGeneratorVersion")
}
