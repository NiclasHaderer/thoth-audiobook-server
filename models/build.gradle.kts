val kotlinxSerializationVersion: String by project

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.ncorti.ktfmt.gradle")
}

dependencies {
    implementation(project(":common"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
}
