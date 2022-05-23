val logbackVersion: String by project
val ktorVersion = "2.0.1"
val openApiVersion = "2.2.0"

plugins {
    application
    kotlin("jvm") version "1.6.21"
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-locations-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    // Openapi
    implementation("io.swagger.core.v3:swagger-models:$openApiVersion")
    implementation("io.swagger.core.v3:swagger-core:$openApiVersion")
    implementation("org.webjars:swagger-ui:4.4.1-1")
}
