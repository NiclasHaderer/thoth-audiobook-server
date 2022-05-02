package io.thoth.server

import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.route
import io.ktor.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.thoth.auth.AuthConfig
import io.thoth.auth.authorization
import io.thoth.common.extensions.shutdown
import io.thoth.models.exceptions.withDefaultErrorHandlers
import io.thoth.server.api.audiobooks.registerAudiobookRouting
import io.thoth.server.api.images.registerImageRouting
import io.thoth.server.api.metadata.registerMetadataRouting
import io.thoth.server.api.search.registerSearchRouting
import io.thoth.server.api.stream.registerStreamingRouting
import io.thoth.server.api.withBasePath
import io.thoth.server.db.DatabaseFactory
import io.thoth.server.file.scanner.CompleteScan
import io.thoth.server.file.scanner.FileChangeService
import io.thoth.server.logging.disableJAudioTaggerLogs
import io.thoth.server.plugins.configureCORS
import io.thoth.server.plugins.configureDevKoin
import io.thoth.server.plugins.configureMonitoring
import io.thoth.server.plugins.configureOpenAPI
import io.thoth.server.plugins.configurePartialContent
import io.thoth.server.plugins.configureProdKoin
import io.thoth.server.plugins.configureRouting
import io.thoth.server.plugins.configureSerialization
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
        authorization(AuthConfig(settings.keyPair, "asdf"))

        try {
            // Has to be done in here for some strange scoping reasons
            DatabaseFactory.connect()
            DatabaseFactory.migrate()
            webServer()
        } catch (e: Exception) {
            log.error("Could not start server", e)
            this.shutdown()
        }

        launch {
            CompleteScan().start()
            FileChangeService().watch()
        }
    }.start(wait = false)
}


fun Application.webServer() {
    configureOpenAPI()
    configureRouting()
    configurePartialContent()
    configureCORS()
    configureSockets()
    configureMonitoring()
    configureSerialization()
    withBasePath("api", routeCallback = { // registerUpdateRoutes()
    }, openApiCallback = {
        apiRouting {
            route("api") {
                withDefaultErrorHandlers {
                    registerMetadataRouting()
                    registerAudiobookRouting()
                    registerSearchRouting()
                    registerStreamingRouting()
                    registerImageRouting()
                }
            }
        }
    })
}

