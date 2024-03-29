import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktorVersion: String by project
val jwtVersion: String by project
val kotlinLoggingVersion: String by project
val springSecurityVersion: String by project
val bouncyCastleVersion: String by project
val joseJWTVersion: String by project
val jwkVersion: String by project


plugins {
    kotlin("jvm")
    id("com.ncorti.ktfmt.gradle")
    id("maven-publish")
}


tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
        jvmTarget = "17"
        apiVersion = "1.8"
        languageVersion = "1.8"
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(":openapi"))
    implementation(project(":auth-models"))

    implementation("io.github.microutils:kotlin-logging-jvm:$kotlinLoggingVersion")

    // Ktor
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-resources:$ktorVersion")
    implementation("io.ktor:ktor-server-data-conversion:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")


    // Security
    implementation("org.springframework.security:spring-security-core:$springSecurityVersion")
    implementation("com.auth0:java-jwt:$jwtVersion")
    implementation("com.auth0:jwks-rsa:$jwkVersion")
    implementation("com.nimbusds:nimbus-jose-jwt:$joseJWTVersion")
    implementation("org.bouncycastle:bcprov-jdk15on:$bouncyCastleVersion")
    implementation("org.bouncycastle:bcpkix-jdk15on:$bouncyCastleVersion")
}



afterEvaluate {
    publishing {
        publications {
            // publish to jitpack
            create<MavenPublication>("maven") {
                groupId = "com.github.niclashaderer"
                artifactId = "auth"
                version = "0.0.1"
                from(components["java"])
            }
        }
    }
}
