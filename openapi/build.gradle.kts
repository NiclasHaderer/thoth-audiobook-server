val ktorVersion: String by project
val kotlinLoggingVersion: String by project
val openApiVersion: String by project
val swaggerUiVersion: String by project

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.ncorti.ktfmt.gradle")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":models"))
    implementation("io.github.microutils:kotlin-logging-jvm:$kotlinLoggingVersion")

    // Ktor
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-resources:$ktorVersion")
    implementation("io.ktor:ktor-server-data-conversion:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    // Openapi
    implementation("io.swagger.core.v3:swagger-models:$openApiVersion")
    implementation("io.swagger.core.v3:swagger-core:$openApiVersion")
    implementation("org.webjars:swagger-ui:$swaggerUiVersion")
    // Gson
    implementation("com.google.code.gson:gson:2.10.1")
}
