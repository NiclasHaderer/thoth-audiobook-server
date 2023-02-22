val reflectVersion: String by project
val exposedVersion: String by project
val springSecurityVersion: String by project
val kotlinLoggingVersion: String by project
val hikariVersion: String by project
val h2Version: String by project
val ktorVersion: String by project

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":models"))
    implementation(project(":common"))
    implementation(project(":config"))
    implementation(project(":metadata"))

    // Database
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("io.github.microutils:kotlin-logging-jvm:$kotlinLoggingVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("com.h2database:h2:$h2Version")

    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")

    implementation("org.reflections:reflections:$reflectVersion")
    implementation("org.springframework.security:spring-security-core:$springSecurityVersion")
}
