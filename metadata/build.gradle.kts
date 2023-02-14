val kotlinVersion: String by project
val ktorVersion: String by project
val jsoupVersion: String by project
val caffeineVersion: String by project
val fuzzyWuzzyVersion: String by project
val logbackVersion: String by project
val kotlinLogging: String by project
val jsonVersion: String by project

plugins {
    kotlin("jvm") version "1.8.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common"))

    // Audible scraping
    implementation("org.jsoup:jsoup:$jsoupVersion")
    implementation("com.github.ben-manes.caffeine:caffeine:$caffeineVersion")
    implementation("me.xdrop:fuzzywuzzy:$fuzzyWuzzyVersion")
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-cio-jvm:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.github.microutils:kotlin-logging-jvm:$kotlinLogging")
    // JSON parsing
    implementation("org.json:json:$jsonVersion")


    // Tests
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
}
