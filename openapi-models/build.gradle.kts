plugins {
    kotlin("jvm")
    id("maven-publish")
}

afterEvaluate {
    publishing {
        publications {
            // publish to jitpack
            create<MavenPublication>("maven") {
                groupId = "com.github.niclashaderer"
                artifactId = "openapi-models"
                version = "0.0.1"
                from(components["java"])
            }
        }
    }
}
