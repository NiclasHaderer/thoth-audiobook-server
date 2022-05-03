val ktorVersion: String by project
val jwtVersion: String by project
val exposedVersion: String by project

plugins {
    kotlin("jvm") version "1.5.31"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":database"))
    implementation(project(":models"))
    implementation(project(":common"))

    // DB
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")

    // JWT
    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-auth-jwt:$ktorVersion")
    implementation("com.auth0:java-jwt:$jwtVersion")
    implementation("com.nimbusds:nimbus-jose-jwt:9.22")
    implementation("org.springframework.security:spring-security-core:5.6.3")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    implementation("io.ktor:ktor-jackson:$ktorVersion")
}
