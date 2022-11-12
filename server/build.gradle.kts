val fuzzyWuzzyVersion: String by project
val fileWatcherVersion: String by project
val ktorVersion: String by project
val kotlinVersion: String by project
val logbackVersion: String by project
val exposedVersion: String by project
val h2Version: String by project
val hikariVersion: String by project
val reflectVersion: String by project
val openApiVersion: String by project
val sqliteVersion: String by project
val koinVersion: String by project
val tsGeneratorVersion: String by project

plugins {
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.serialization") version "1.6.21"
    application
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

// Shadow task depends on Jar task, so these configs are reflected for Shadow as well
tasks.jar {
    manifest.attributes["Main-Class"] = "io.thoth.server.ApplicationKt"
}

application {
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=true")
    mainClass.set("io.thoth.server.ApplicationKt")
    tasks.run.get().workingDir = rootProject.projectDir
}

// For kotlin annotations
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
        freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
        jvmTarget = "11"
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }
}



repositories {
    maven("https://jitpack.io")
    mavenCentral()
}

dependencies {
    // Other projects
    implementation(project(":authentication"))
    implementation(project(":common"))
    implementation(project(":models"))
    implementation(project(":openapi"))
    implementation(project(":database"))
    implementation(project(":metadata"))

    // Database
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    // Config
    implementation("com.sksamuel.hoplite:hoplite-core:2.6.2")
    implementation("com.sksamuel.hoplite:hoplite-json:2.6.2")
    implementation("com.sksamuel.hoplite:hoplite-yaml:2.6.2")
    implementation("com.sksamuel.hoplite:hoplite-toml:2.6.2")
    implementation("com.sksamuel.hoplite:hoplite-hocon:2.6.2")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
    // Database drivers
    implementation("org.xerial:sqlite-jdbc:$sqliteVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    // Pined, because of exposed compatability
    implementation("com.h2database:h2:$h2Version")
    // Dependency Injection
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")
    // Migration
    implementation("org.reflections:reflections:$reflectVersion")
    // Audio file processing
    implementation("org.bitbucket.ijabz:jaudiotagger:v3.0.1")
    // Folder watching
    implementation("io.methvin:directory-watcher:$fileWatcherVersion")
    // Search
    implementation("me.xdrop:fuzzywuzzy:$fuzzyWuzzyVersion")
    // Logging
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    // Ktor
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-partial-content:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-sessions-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-host-common-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-resources:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-locations-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-cio-jvm:$ktorVersion")
    // Tests
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktorVersion")
}
