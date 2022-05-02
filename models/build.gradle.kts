val openApiVersion: String by project
val ktorVersion: String by project

plugins {
    application
    kotlin("jvm") version "1.5.31"
}

repositories {
    maven("https://jitpack.io")
    mavenCentral()
}

dependencies {
    implementation(project(":metadata"))

    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("com.github.papsign:Ktor-OpenAPI-Generator:$openApiVersion")

}
