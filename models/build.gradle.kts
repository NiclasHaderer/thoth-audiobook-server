val kotlinxSerializationVersion: String by project
val exposedVersion: String by project

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.ncorti.ktfmt.gradle")
}

dependencies {
    implementation(project(":auth"))
    implementation(project(":auth-models"))
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
}
