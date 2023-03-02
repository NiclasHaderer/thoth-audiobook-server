rootProject.name = "thoth-audiobook-server"

include(
    "authentication",
    "common",
    "database",
    "generators",
    "metadata",
    "models",
    "openapi",
    "server",
    "config")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }
}
