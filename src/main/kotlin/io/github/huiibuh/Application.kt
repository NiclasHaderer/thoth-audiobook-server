package io.github.huiibuh

import api.exceptions.withDefaultErrorHandlers
import com.papsign.ktor.openapigen.route.apiRouting
import io.github.huiibuh.api.audible.registerAudibleRouting
import io.github.huiibuh.api.audiobooks.registerAudiobookRouting
import io.github.huiibuh.api.stream.registerStreamingRouting
import io.github.huiibuh.db.DatabaseFactory
import io.github.huiibuh.logging.disableJAudioTaggerLogs
import io.github.huiibuh.plugins.*
import io.github.huiibuh.services.DB
import io.ktor.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*


fun main() {
    disableJAudioTaggerLogs()
    DatabaseFactory.connectAndMigrate()
    DB.runValidation()
    DB.importMissingTracks()

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}


fun Application.module() {
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
        }
    }

}
