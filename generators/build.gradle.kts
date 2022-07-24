val tsGeneratorVersion: String by project
val kotlinVersion: String by project

plugins {
    application
    kotlin("jvm") version "1.6.21"
}

repositories {
//    maven("https://jitpack.io")
    mavenCentral()
}

dependencies {
//    implementation(project(":metadata"))
//    implementation(project(":models"))
//    implementation(project(":server"))
    // Typescript code generation
//    implementation("com.github.ntrrgc:ts-generator:$tsGeneratorVersion")
//    implementation("cc.vileda:kotlin-openapi3-dsl:1.2.0")
//    implementation("org.json:json:20220320")
//    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
}
