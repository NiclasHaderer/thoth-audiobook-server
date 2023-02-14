val ktorVersion: String by project
val kotlinLogging: String by project
val openApiVersion: String by project
val swaggerUiVersion: String by project

plugins {
    kotlin("jvm") version "1.8.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common"))
    implementation("io.github.microutils:kotlin-logging-jvm:$kotlinLogging")

    // Ktor
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-resources:$ktorVersion")
    implementation("io.ktor:ktor-server-data-conversion:$ktorVersion")
    // Openapi
    implementation("io.swagger.core.v3:swagger-models:$openApiVersion")
    implementation("io.swagger.core.v3:swagger-core:$openApiVersion")
    implementation("org.webjars:swagger-ui:$swaggerUiVersion")
}
