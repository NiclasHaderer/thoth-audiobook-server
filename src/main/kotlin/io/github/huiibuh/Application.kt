package io.github.huiibuh

import api.exceptions.withDefaultErrorHandlers
import com.papsign.ktor.openapigen.route.apiRouting
import io.github.huiibuh.api.audible.registerAudibleRouting
import io.github.huiibuh.api.audiobooks.registerAudiobookRouting
import io.github.huiibuh.api.images.registerImageRouting
import io.github.huiibuh.api.stream.registerStreamingRouting
import io.github.huiibuh.config.Settings
import io.github.huiibuh.db.DatabaseFactory
import io.github.huiibuh.logging.disableJAudioTaggerLogs
import io.github.huiibuh.plugins.*
import io.github.huiibuh.services.Cache
import io.ktor.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*


fun main() {
    disableJAudioTaggerLogs()
    DatabaseFactory.connectAndMigrate()
    Cache.reinitialize()
    embeddedServer(Netty, port = Settings.webUiPort, host = "0.0.0.0") {
        webServer()
    }.start(wait = true)
}


fun Application.webServer() {
    configureOpenAPI()
    configureRouting()
    configureHTTP()
    configureMonitoring()
    configureSerialization()
    apiRouting {
        withDefaultErrorHandlers {
            registerAudibleRouting()
            registerAudiobookRouting()
            registerStreamingRouting()
            registerImageRouting()
        }
    }
}
