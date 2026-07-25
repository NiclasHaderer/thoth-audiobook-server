plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    alias(libs.plugins.shadow)
    application
}

application {
    // :taglib reaches TagLib through the FFM API, which needs native access granted.
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=true", "--enable-native-access=ALL-UNNAMED")
    mainClass.set("io.thoth.server.ApplicationKt")
    tasks.run.get().workingDir = rootProject.projectDir
}

// Shadow task depends on Jar task, so these configs are reflected for Shadow as well
tasks.jar {
    manifest.attributes["Main-Class"] = "io.thoth.server.ApplicationKt"
    manifest.attributes["Enable-Native-Access"] = "ALL-UNNAMED"
}

dependencies {
    // Other projects
    implementation(project(":openapi"))
    implementation(project(":auth"))
    implementation(project(":auth-models"))

    // Metadata
    implementation(libs.jsoup)
    implementation(libs.caffeine)
    implementation(libs.json)

    // Database
    implementation(libs.bundles.exposed)
    // Drivers
    implementation(libs.sqlite.jdbc)
    implementation(libs.hikaricp)
    // Migration
    implementation(libs.classgraph)

    // JWT
    implementation(libs.java.jwt)
    implementation(libs.nimbus.jose.jwt)
    implementation(libs.spring.security.core)
    implementation(libs.bundles.bouncycastle)

    // Config
    implementation(libs.bundles.hoplite)

    // Serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.serialization.jackson)

    // Dependency Injection
    implementation(libs.bundles.koin)

    // Audio file processing
    implementation(project(":taglib"))
    // Folder watching
    implementation(libs.directory.watcher)
    // Search
    implementation(libs.fuzzywuzzy)
    // Logging
    implementation(libs.logback.classic)
    implementation(libs.kotlin.logging)
    implementation(libs.slf4j.jul.to.slf4j)
    // Scheduler
    implementation(libs.cron.utils)

    // Ktor
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.partial.content)
    implementation(libs.ktor.server.data.conversion)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.content.negotiation.jvm)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.core.jvm)
    implementation(libs.ktor.server.auth.jvm)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.sessions.jvm)
    implementation(libs.ktor.server.host.common.jvm)
    implementation(libs.ktor.server.resources)
    implementation(libs.ktor.server.websockets.jvm)
    implementation(libs.ktor.server.netty.jvm)
    implementation(libs.ktor.client.core.jvm)
    implementation(libs.ktor.client.cio.jvm)

    // Openapi
    implementation(libs.swagger.models)
    // Tests
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.ktor.server.test.host)
}
