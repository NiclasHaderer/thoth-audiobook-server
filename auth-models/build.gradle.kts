plugins {
    kotlin("jvm")
    id("com.ncorti.ktfmt.gradle")
    id("maven-publish")
}

repositories {
    mavenCentral()
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
