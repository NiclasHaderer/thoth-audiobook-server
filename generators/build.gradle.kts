val tsGeneratorVersion: String by project

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":metadata"))
    implementation(project(":models"))
    implementation(project(":server"))
    // Typescript code generation
    implementation("com.github.ntrrgc:ts-generator:$tsGeneratorVersion")
}
