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
                artifactId = "auth"
                version = "0.0.1"
                from(components["java"])
            }
        }
    }
}
