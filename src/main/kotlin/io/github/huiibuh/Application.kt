package io.github.huiibuh

import io.github.huiibuh.api.audible.registerAudibleRouting
import io.github.huiibuh.api.audiobooks.registerAudiobookRouting
import io.github.huiibuh.dependencies.configureKoin
import io.github.huiibuh.plugins.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureKoin()
        configureOpenAPI()
        configureRouting()
        configureHTTP()
        configureMonitoring()
        configureSerialization()
        registerAudibleRouting()
        registerAudiobookRouting()
        connectToDatabase()
    }.start(wait = true)
}
