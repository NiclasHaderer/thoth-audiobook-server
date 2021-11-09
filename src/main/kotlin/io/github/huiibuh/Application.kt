package io.github.huiibuh

import io.github.huiibuh.api.audible.registerAudibleRouting
import io.github.huiibuh.api.audiobooks.registerAudiobookRouting
import io.github.huiibuh.db.DatabaseFactory
import io.github.huiibuh.logging.disableJAudioTaggerLogs
import io.github.huiibuh.plugins.*
import io.github.huiibuh.services.DB
import io.ktor.server.engine.*
import io.ktor.server.netty.*


fun main() {
    disableJAudioTaggerLogs()
    DatabaseFactory.connectAndMigrate()
    DB.runValidation()
    DB.importMissingTracks()

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureOpenAPI()
        configureRouting()
        configureHTTP()
        configureMonitoring()
        configureSerialization()
        registerAudibleRouting()
        registerAudiobookRouting()
    }.start(wait = true)
}
