val ktorVersion: String by project
val jsoupVersion: String by project
val caffeineVersion: String by project
val fuzzyWuzzyVersion: String by project

plugins {
    kotlin("jvm") version "1.6.21"
}

repositories {
    mavenCentral()
}

dependencies {
    // Audible scraping
    implementation("org.jsoup:jsoup:$jsoupVersion")
    implementation("com.github.ben-manes.caffeine:caffeine:$caffeineVersion")
    implementation("me.xdrop:fuzzywuzzy:$fuzzyWuzzyVersion")
    implementation("io.ktor:ktor-client-core-jvm:2.0.2")
    implementation("io.ktor:ktor-client-cio-jvm:2.0.2")
}
