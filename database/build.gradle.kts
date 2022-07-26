val reflectVersion: String by project
val exposedVersion: String by project
val springSecurityVersion: String by project

plugins {
    kotlin("jvm") version "1.6.21"
}

repositories {
    maven("https://jitpack.io")
    mavenCentral()
}

dependencies {
    implementation(project(":models"))
    implementation(project(":common"))
    implementation(project(":metadata"))

    // Database
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    implementation("org.reflections:reflections:$reflectVersion")
    implementation("org.springframework.security:spring-security-core:$springSecurityVersion")
}
