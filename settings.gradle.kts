rootProject.name = "thoth-audiobook-server"

include(
    "openapi",
    "metadata",
    "models",
    "server",
    "auth"
)

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }
}
