val fuzzyWuzzyVersion: String by project
val ktorVersion: String by project
val kotlinVersion: String by project
val logbackVersion: String by project
val exposedVersion: String by project
val h2Version: String by project
val hikariVersion: String by project
val reflectVersion: String by project
val sqliteVersion: String by project
val koinVersion: String by project
val jacksonVersion: String by project

plugins {
    application
    kotlin("jvm") version "1.5.31"
}

application {
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=true")
    mainClass.set("io.github.huiibuh.ApplicationKt")
}

// For kotlin annotations
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
    // Other projects
    implementation(project(":database"))
    implementation(project(":metadata"))

    // Database
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    // Database drivers
    implementation("org.xerial:sqlite-jdbc:$sqliteVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    // Pined, because of exposed compatability
    implementation("com.h2database:h2:1.4.199")
    // Dependency Injection
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")
    // Migration
    implementation("org.reflections:reflections:$reflectVersion")
    // OpenAPI
    implementation("com.github.papsign:Ktor-OpenAPI-Generator:0.3-beta.2")
    // Typescript code generation
    implementation("com.github.ntrrgc:ts-generator:1.1.1")
    // Audio file processing
    implementation("org.bitbucket.ijabz:jaudiotagger:v3.0.1")
    // Folder watching
    implementation("io.methvin:directory-watcher:0.15.0")
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
    // Search
    implementation("me.xdrop:fuzzywuzzy:$fuzzyWuzzyVersion")
    // Logging
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    // Tests
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")

}
