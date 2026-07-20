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
                artifactId = "client"
                version = "0.0.1"
                from(components["java"])
            }
        }
    }
}

dependencies {
    implementation(libs.arrow.core)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.kotlin.reflect)
}
