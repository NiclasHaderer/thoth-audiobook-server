val reflectVersion: String by project
val exposedVersion: String by project
val springSecurityVersion: String by project
val kotlinLogging: String by project
val hikariVersion: String by project

plugins {
    kotlin("jvm") version "1.8.0"
}

repositories {
    maven("https://jitpack.io")
    mavenCentral()
}

dependencies {
    implementation(project(":models"))
    implementation(project(":common"))
    implementation(project(":config"))
    implementation(project(":metadata"))

    // Database
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    // TODO remove
    implementation("io.github.microutils:kotlin-logging-jvm:$kotlinLogging")
    implementation("com.zaxxer:HikariCP:$hikariVersion")



    implementation("org.reflections:reflections:$reflectVersion")
    implementation("org.springframework.security:spring-security-core:$springSecurityVersion")
}
