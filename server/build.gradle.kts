val fuzzyWuzzyVersion: String by project
val fileWatcherVersion: String by project
val ktorVersion: String by project
val kotlinVersion: String by project
val logbackVersion: String by project
val exposedVersion: String by project
val reflectVersion: String by project
val sqliteVersion: String by project
val koinVersion: String by project
val kotlinLoggingVersion: String by project
val slf4jVersion: String by project
val kotlinxSerializationVersion: String by project
val jAudioTaggerVersion: String by project
val cronUtilsVersion: String by project
val openApiVersion: String by project
val hopliteVersion: String by project
val jwtVersion: String by project
val springSecurityVersion: String by project
val bouncyCastleVersion: String by project
val joseJWTVersion: String by project
val hikariVersion: String by project
val caffeineVersion: String by project
val jsoupVersion: String by project
val jsonVersion: String by project

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow") version "7.1.2"
    application
}

application {
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=true")
    mainClass.set("io.thoth.server.ApplicationKt")
    tasks.run.get().workingDir = rootProject.projectDir
}

// Shadow task depends on Jar task, so these configs are reflected for Shadow as well
tasks.jar { manifest.attributes["Main-Class"] = "io.thoth.server.ApplicationKt" }

dependencies {
    // Other projects
    implementation(project(":openapi"))
    implementation(project(":auth"))
    implementation(project(":auth-models"))

    // Metadata
    implementation("org.jsoup:jsoup:$jsoupVersion")
    implementation("com.github.ben-manes.caffeine:caffeine:$caffeineVersion")
    implementation("org.json:json:$jsonVersion")

    // Database
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    // Drivers
    implementation("org.xerial:sqlite-jdbc:$sqliteVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    // Migration
    implementation("org.reflections:reflections:$reflectVersion")

    // JWT
    implementation("com.auth0:java-jwt:$jwtVersion")
    implementation("com.nimbusds:nimbus-jose-jwt:$joseJWTVersion")
    implementation("org.springframework.security:spring-security-core:$springSecurityVersion")
    implementation("org.bouncycastle:bcprov-jdk15on:$bouncyCastleVersion")
    implementation("org.bouncycastle:bcpkix-jdk15on:$bouncyCastleVersion")

    // Config
    implementation("com.sksamuel.hoplite:hoplite-core:$hopliteVersion")
    implementation("com.sksamuel.hoplite:hoplite-json:$hopliteVersion")
    implementation("com.sksamuel.hoplite:hoplite-yaml:$hopliteVersion")
    implementation("com.sksamuel.hoplite:hoplite-toml:$hopliteVersion")
    implementation("com.sksamuel.hoplite:hoplite-hocon:$hopliteVersion")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")

    // Dependency Injection
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")

    // Audio file processing
    implementation("org.bitbucket.ijabz:jaudiotagger:$jAudioTaggerVersion")
    // Folder watching
    implementation("io.methvin:directory-watcher:$fileWatcherVersion")
    // Search
    implementation("me.xdrop:fuzzywuzzy:$fuzzyWuzzyVersion")
    // Logging
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.github.microutils:kotlin-logging-jvm:$kotlinLoggingVersion")
    implementation("org.slf4j:jul-to-slf4j:$slf4jVersion")
    // Scheduler
    implementation("com.cronutils:cron-utils:$cronUtilsVersion")

    // Ktor
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-partial-content:$ktorVersion")
    implementation("io.ktor:ktor-server-data-conversion:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-sessions-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-host-common-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-resources:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-cio-jvm:$ktorVersion")

    // Openapi
    implementation("io.swagger.core.v3:swagger-models:$openApiVersion")
    // Tests
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktorVersion")
}
