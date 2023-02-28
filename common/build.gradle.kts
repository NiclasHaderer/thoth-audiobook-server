val ktorVersion: String by project
val koinVersion: String by project
val fuzzyWuzzyVersion: String by project
val reflectVersion: String by project
val exposedVersion: String by project
val kotlinLoggingVersion: String by project
val kotlinxSerializationVersion: String by project
val cronUtilsVersion: String by project

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    // Search
    implementation("me.xdrop:fuzzywuzzy:$fuzzyWuzzyVersion")
    // Database
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    // Ktor
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-cio-jvm:$ktorVersion")
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    // Logging
    implementation("io.github.microutils:kotlin-logging-jvm:$kotlinLoggingVersion")

    // Scheduling
    implementation("com.cronutils:cron-utils:$cronUtilsVersion")
}
