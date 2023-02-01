val reflectVersion: String by project
val exposedVersion: String by project
val springSecurityVersion: String by project
val h2Version: String by project
val sqliteVersion: String by project
val logbackVersion: String by project

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
    // TODO remove
    implementation("com.h2database:h2:$h2Version")
    implementation("org.xerial:sqlite-jdbc:$sqliteVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")


    implementation("org.reflections:reflections:$reflectVersion")
    implementation("org.springframework.security:spring-security-core:$springSecurityVersion")
}
