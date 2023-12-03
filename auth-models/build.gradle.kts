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

afterEvaluate {
    publishing {
        publications {
            // publish to jitpack
            create<MavenPublication>("maven") {
                groupId = "com.github.niclashaderer"
                artifactId = "auth-models"
                version = "0.0.1"
                from(components["java"])
            }
        }
    }
}
