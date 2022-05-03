val ktorVersion: String by project
val koinVersion: String by project
val openApiVersion: String by project
val fuzzyWuzzyVersion: String by project
val reflectVersion: String by project
val exposedVersion: String by project

plugins {
    kotlin("jvm") version "1.5.31"
}

repositories {
    maven("https://jitpack.io")
    mavenCentral()
}

dependencies {
    implementation(project(":models"))
    // Search
    implementation("me.xdrop:fuzzywuzzy:$fuzzyWuzzyVersion")
    // Ktor
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")

    // Dependency Injection
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")
    // OpenAPI
    implementation("com.github.papsign:Ktor-OpenAPI-Generator:$openApiVersion")
    // Database
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    implementation("org.reflections:reflections:$reflectVersion")

    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("com.github.papsign:Ktor-OpenAPI-Generator:$openApiVersion")

}
