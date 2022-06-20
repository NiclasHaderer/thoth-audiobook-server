package io.thoth.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.thoth.common.extensions.shutdown
import io.thoth.openapi.configureStatusPages
import io.thoth.server.api.audiobooks.registerAudiobookRouting
import io.thoth.server.api.images.registerImageRouting
import io.thoth.server.api.metadata.registerMetadataRouting
import io.thoth.server.api.search.registerSearchRouting
import io.thoth.server.api.stream.registerStreamingRouting
import io.thoth.server.db.DatabaseFactory
import io.thoth.server.file.scanner.CompleteScan
import io.thoth.server.file.scanner.FileChangeService
import io.thoth.server.logging.disableJAudioTaggerLogs
import io.thoth.server.plugins.configureCORS
import io.thoth.server.plugins.configureDevKoin
import io.thoth.server.plugins.configureMonitoring
import io.thoth.server.plugins.configurePartialContent
import io.thoth.server.plugins.configureProdKoin
import io.thoth.server.plugins.configureRouting
import io.thoth.server.plugins.configureSockets
import io.thoth.server.settings.Settings
import io.thoth.server.settings.getPort
import io.thoth.server.settings.isProduction
import kotlinx.coroutines.launch
import org.koin.ktor.ext.inject

fun main() {
    disableJAudioTaggerLogs()
    embeddedServer(
        Netty, port = getPort(), watchPaths = listOf("classes"), host = "0.0.0.0"
    ) {
        if (isProduction()) configureProdKoin() else configureDevKoin()
        val settings by inject<Settings>()

        // TODO
        //        authentication(AuthConfig(settings.keyPair, "asd", "http://0.0.0.0:${settings.webUiPort}"))

        try {
            DatabaseFactory.connect()
            DatabaseFactory.migrate()
            webServer()
        } catch (e: Exception) {
            log.error("Could not start server", e)
            shutdown()
        }
        launch {
            FileChangeService().watch()
            CompleteScan().start()
        }
    }.start(wait = true)
}


fun Application.webServer() {
    configureStatusPages()
    configureRouting()
    configurePartialContent()
    configureCORS()
    configureSockets()
    configureMonitoring()
    routing {
        route("api") {
            registerMetadataRouting()
            registerAudiobookRouting()
            registerSearchRouting()
            registerStreamingRouting()
            registerImageRouting()
        }
    }
}

