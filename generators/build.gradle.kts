val tsGeneratorVersion: String by project
val ktorVersion: String by project
val kotlinLoggingVersion: String by project

plugins {
    kotlin("jvm")
    id("com.ncorti.ktfmt.gradle")
}

dependencies {
    implementation(project(":openapi"))
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.github.microutils:kotlin-logging-jvm:$kotlinLoggingVersion")
}
