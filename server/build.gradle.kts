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
val jacksonVersion: String by project
val tsGeneratorVersion: String by project

plugins {
    application
    kotlin("jvm") version "1.6.21"
}

application {
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=true")
    mainClass.set("io.thoth.ApplicationKt")
}

// For kotlin annotations
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
        freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
    }
}

repositories {
    maven("https://jitpack.io")
    mavenCentral()
}

dependencies {
    // Other projects
//    implementation(project(":authentication"))
    implementation(project(":common"))
    implementation(project(":database"))
    implementation(project(":metadata"))
    implementation(project(":models"))

    // Database
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
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
    // OpenAPI
    implementation("com.github.papsign:Ktor-OpenAPI-Generator:$openApiVersion")
    // Audio file processing
    implementation("org.bitbucket.ijabz:jaudiotagger:v3.0.1")
    // Folder watching
    implementation("io.methvin:directory-watcher:$fileWatcherVersion")
    // Ktor
    // Search
    implementation("me.xdrop:fuzzywuzzy:$fuzzyWuzzyVersion")
    // Logging
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.ktor:ktor-server-cors:2.0.2")
    implementation("io.ktor:ktor-server-partial-content:2.0.2")
    implementation("io.ktor:ktor-server-call-logging:2.0.2")
    implementation("io.ktor:ktor-server-content-negotiation:2.0.2")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.0.2")
    implementation("io.ktor:ktor-server-core-jvm:2.0.2")
    implementation("io.ktor:ktor-server-auth-jvm:2.0.2")
    implementation("io.ktor:ktor-server-sessions-jvm:2.0.2")
    implementation("io.ktor:ktor-server-host-common-jvm:2.0.2")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.0.2")
    implementation("io.ktor:ktor-server-websockets-jvm:2.0.2")
    implementation("io.ktor:ktor-server-locations-jvm:2.0.2")
    implementation("io.ktor:ktor-server-netty-jvm:2.0.2")
    implementation("io.ktor:ktor-client-core-jvm:2.0.2")
    implementation("io.ktor:ktor-client-cio-jvm:2.0.2")
    // Tests
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    testImplementation("io.ktor:ktor-server-tests-jvm:2.0.2")
}
