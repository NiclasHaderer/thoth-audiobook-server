val ktor2Version: String by project

val jwtVersion: String by project
val exposedVersion: String by project

plugins {
    kotlin("jvm") version "1.6.21"
}

repositories {
    maven("https://jitpack.io")
    mavenCentral()
}

dependencies {
    implementation(project(":database"))
    implementation(project(":models"))
    implementation(project(":common"))
    implementation(project(":openapi"))

    // DB
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")

    // Ktor
    implementation("io.ktor:ktor-server-auth:$ktor2Version")
    implementation("io.ktor:ktor-server-auth-jwt:$ktor2Version")
    implementation("io.ktor:ktor-jackson:$ktor2Version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor2Version")
    implementation("io.ktor:ktor-serialization:$ktor2Version")
    implementation("io.ktor:ktor-server-core-jvm:$ktor2Version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor2Version")
    implementation("io.ktor:ktor-server-status-pages:$ktor2Version")
    implementation("io.ktor:ktor-server-locations-jvm:$ktor2Version")
    implementation("io.ktor:ktor-serialization-jackson:$ktor2Version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor2Version")

    // JWT
    implementation("com.auth0:java-jwt:$jwtVersion")
    implementation("com.nimbusds:nimbus-jose-jwt:9.22")
    implementation("org.springframework.security:spring-security-core:5.6.3")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
}
