val ktorVersion: String by project
val jsoupVersion: String by project
val caffeineVersion: String by project
val fuzzyWuzzyVersion: String by project

plugins {
    kotlin("jvm") version "1.5.31"
}

repositories {
    mavenCentral()
}

dependencies{
    // Audible scraping
    implementation("org.jsoup:jsoup:$jsoupVersion")
    implementation("com.github.ben-manes.caffeine:caffeine:$caffeineVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("me.xdrop:fuzzywuzzy:$fuzzyWuzzyVersion")

}
