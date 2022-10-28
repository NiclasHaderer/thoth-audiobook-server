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
import io.thoth.server.config.loadConfig
import io.thoth.server.db.DatabaseFactory
import io.thoth.server.file.scanner.CompleteScan
import io.thoth.server.file.scanner.FileChangeService
import io.thoth.server.logging.disableJAudioTaggerLogs
import io.thoth.server.plugins.configureCORS
import io.thoth.server.plugins.configureKoin
import io.thoth.server.plugins.configureMonitoring
import io.thoth.server.plugins.configureOpenApi
import io.thoth.server.plugins.configurePartialContent
import io.thoth.server.plugins.configureRouting
import io.thoth.server.plugins.configureSerialization
import io.thoth.server.plugins.configureSockets
import kotlinx.coroutines.launch

fun main() {
    disableJAudioTaggerLogs()
    val config = loadConfig()
    embeddedServer(
        Netty, port = config.webUiPort, watchPaths = listOf("classes"), host = "0.0.0.0"
    ) {
        configureKoin(config)

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
        }
        launch {
            CompleteScan().start()
        }
    }.start(wait = true)
}

fun Application.webServer() {
    configureStatusPages()
    configureRouting()
    configureOpenApi()
    configurePartialContent()
    configureCORS()
    configureSockets()
    configureMonitoring()
    configureSerialization()

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

