val jwtVersion: String by project
val exposedVersion: String by project
val ktorVersion: String by project
val springSecurityVersion: String by project


plugins {
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.serialization") version "1.6.21"
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
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-resources:$ktorVersion")

    // JWT
    implementation("com.auth0:java-jwt:$jwtVersion")
    implementation("com.nimbusds:nimbus-jose-jwt:9.22")
    implementation("org.springframework.security:spring-security-core:$springSecurityVersion")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")
}
