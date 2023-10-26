rootProject.name = "thoth-audiobook-server"

include(
    "openapi",
    "metadata",
    "models",
    "server",
    "auth",
    "auth-models"
)

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }
}
