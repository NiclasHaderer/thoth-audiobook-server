package io.github.huiibuh

import io.github.huiibuh.plugins.*
import io.github.huiibuh.routing.registerAudiobookRouting
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureOpenAPI()
        configureRouting()
        configureHTTP()
        configureMonitoring()
        configureSerialization()
        registerAudiobookRouting()
    }.start(wait = true)
}
