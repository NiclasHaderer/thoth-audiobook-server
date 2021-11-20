val ktorVersion: String by project
val kotlinVersion: String by project
val logbackVersion: String by project
val jsoupVersion: String by project
val exposedVersion: String by project
val h2Version: String by project
val hikariVersion: String by project
val reflectVersion: String by project
val sqliteVersion: String by project

plugins {
    application
    kotlin("jvm") version "1.5.31"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.5.31"
}

group = "io.github.huiibuh"
version = "0.0.1"
application {
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=true")
    mainClass.set("io.github.huiibuh.ApplicationKt")
}


tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }
}

repositories {
    maven("https://jitpack.io")
    mavenCentral()
}

dependencies {
    // Database
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.xerial:sqlite-jdbc:$sqliteVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    // Migration
    implementation("org.reflections:reflections:$reflectVersion")
    // OpenAPI
    implementation("com.github.papsign:Ktor-OpenAPI-Generator:0.3-beta.2")
    // Audio file processing
    implementation("org.bitbucket.ijabz:jaudiotagger:v3.0.1")
    // Ktor
    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-sessions:$ktorVersion")
    implementation("io.ktor:ktor-server-host-common:$ktorVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")
    implementation("io.ktor:ktor-websockets:$ktorVersion")
    implementation("io.ktor:ktor-locations:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    // Audible scraping
    implementation("org.jsoup:jsoup:$jsoupVersion")
    // Logging
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    // Tests
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    testImplementation("com.h2database:h2:$h2Version")
    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")

}
