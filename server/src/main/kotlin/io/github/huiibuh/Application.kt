package io.github.huiibuh

import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.route
import io.github.huiibuh.api.audiobooks.registerAudiobookRouting
import io.github.huiibuh.api.exceptions.withDefaultErrorHandlers
import io.github.huiibuh.api.images.registerImageRouting
import io.github.huiibuh.api.metadata.registerMetadataRouting
import io.github.huiibuh.api.search.registerSearchRouting
import io.github.huiibuh.api.stream.registerStreamingRouting
import io.github.huiibuh.api.withBasePath
import io.github.huiibuh.db.DatabaseFactory
import io.github.huiibuh.plugins.configureProdKoin
import io.github.huiibuh.file.scanner.CompleteScan
import io.github.huiibuh.file.scanner.FileChangeService
import io.github.huiibuh.logging.disableJAudioTaggerLogs
import io.github.huiibuh.plugins.configureCORS
import io.github.huiibuh.plugins.configureMonitoring
import io.github.huiibuh.plugins.configureOpenAPI
import io.github.huiibuh.plugins.configurePartialContent
import io.github.huiibuh.plugins.configureRouting
import io.github.huiibuh.plugins.configureSerialization
import io.github.huiibuh.plugins.configureSockets
import io.github.huiibuh.settings.getPort
import io.ktor.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.launch

fun main() {
    disableJAudioTaggerLogs()
    embeddedServer(
        Netty, port = getPort(), watchPaths = listOf("classes"), host = "0.0.0.0"
    ) { // Has to be done in here for some strange scoping reasons
        configureProdKoin()
        launch {
            connectToDB()
        }
        webServer()
    }.start(wait = false)
}

fun connectToDB() {
    DatabaseFactory.connect()
    DatabaseFactory.migrate()
    CompleteScan().start()
    FileChangeService().watch()
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
