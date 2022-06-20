val logbackVersion: String by project
val ktorVersion: String by project
val ktor2Version: String by project
val openApiVersion = "2.2.0"

plugins {
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.serialization") version "1.6.21"
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor
    implementation("io.ktor:ktor-server-content-negotiation:$ktor2Version")
    implementation("io.ktor:ktor-server-core-jvm:$ktor2Version")
    implementation("io.ktor:ktor-server-resources:$ktor2Version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor2Version")
    implementation("io.ktor:ktor-server-status-pages:$ktor2Version")
    implementation("io.ktor:ktor-server-locations-jvm:$ktor2Version")
    implementation("io.ktor:ktor-serialization-jackson:$ktor2Version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor2Version")
    // Openapi
    implementation("io.swagger.core.v3:swagger-models:$openApiVersion")
    implementation("io.swagger.core.v3:swagger-core:$openApiVersion")
    implementation("org.webjars:swagger-ui:4.4.1-1")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.0.2")
}
