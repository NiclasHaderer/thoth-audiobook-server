rootProject.name = "thoth-audiobook-server"

include(
    "generators",
    "metadata",
    "models",
    "server",
)

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }
}
