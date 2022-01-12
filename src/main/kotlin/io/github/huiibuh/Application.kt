package io.github.huiibuh

import com.papsign.ktor.openapigen.route.apiRouting
import io.github.huiibuh.api.audiobooks.registerAudiobookRouting
import io.github.huiibuh.api.exceptions.withDefaultErrorHandlers
import io.github.huiibuh.api.images.registerImageRouting
import io.github.huiibuh.api.metadata.registerMetadataRouting
import io.github.huiibuh.api.search.registerSearchRouting
import io.github.huiibuh.api.stream.registerStreamingRouting
import io.github.huiibuh.db.DatabaseFactory
import io.github.huiibuh.di.configureKoin
import io.github.huiibuh.file.scanner.UpdateService
import io.github.huiibuh.logging.disableJAudioTaggerLogs
import io.github.huiibuh.plugins.configureHTTP
import io.github.huiibuh.plugins.configureMonitoring
import io.github.huiibuh.plugins.configureOpenAPI
import io.github.huiibuh.plugins.configurePartialContent
import io.github.huiibuh.plugins.configureRouting
import io.github.huiibuh.plugins.configureSerialization
import io.github.huiibuh.plugins.configureSockets
import io.github.huiibuh.services.Scanner
import io.github.huiibuh.settings.getPort
import io.github.huiibuh.ws.registerUpdateRoutes
import io.ktor.application.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.launch


fun main() {
    disableJAudioTaggerLogs()
    embeddedServer(Netty, port = getPort(), host = "0.0.0.0") {
        // Has to be done in here for some strange scoping reasons
        configureKoin()
        launch {
            DatabaseFactory.connect()
            DatabaseFactory.migrate()
            Scanner.rescan()
            UpdateService().watch()
        }
        webServer()
    }.start(wait = false)
}


fun Application.webServer() {
    configureOpenAPI()
    configureRouting()
    configurePartialContent()
    configureHTTP()
    configureSockets()
    configureMonitoring()
    configureSerialization()
    routing {
        registerUpdateRoutes()
    }
    apiRouting {
        withDefaultErrorHandlers {
            registerMetadataRouting()
            registerAudiobookRouting()
            registerSearchRouting()
            registerStreamingRouting()
            registerImageRouting()
        }
    }
}
