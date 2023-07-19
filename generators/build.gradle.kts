val tsGeneratorVersion: String by project
val ktorVersion: String by project
val kotlinLoggingVersion: String by project
val openApiVersion: String by project
val swaggerUiVersion: String by project
val kotlinVersion: String by project

plugins {
    kotlin("jvm")
    id("com.ncorti.ktfmt.gradle")
    id("maven-publish")
}

dependencies {
    implementation("io.github.microutils:kotlin-logging-jvm:$kotlinLoggingVersion")

    // Ktor
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-resources:$ktorVersion")
    implementation("io.ktor:ktor-server-data-conversion:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")

    // Openapi
    implementation("io.swagger.core.v3:swagger-models:$openApiVersion")
    implementation("io.swagger.core.v3:swagger-core:$openApiVersion")
    implementation("org.webjars:swagger-ui:$swaggerUiVersion")

    // Tests
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
}

afterEvaluate {
    publishing {
        publications {
            // publish to jitpack
            create<MavenPublication>("maven") {
                groupId = "com.github.niclashaderer"
                artifactId = "openapi"
                version = tsGeneratorVersion

                from(components["java"])
            }
        }
    }
}
