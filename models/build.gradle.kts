val openApiVersion: String by project
val ktorVersion: String by project

plugins {
    application
    kotlin("jvm") version "1.6.21"
}

repositories {
    maven("https://jitpack.io")
    mavenCentral()
}

dependencies {
}
