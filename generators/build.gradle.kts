val tsGeneratorVersion: String by project
val ktorVersion: String by project

plugins {
    kotlin("jvm")
    id("com.ncorti.ktfmt.gradle")
}

dependencies {
    implementation(project(":openapi"))
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    // Typescript code generation
    implementation("com.github.ntrrgc:ts-generator:$tsGeneratorVersion")
}
