package io.github.huiibuh

import api.exceptions.withDefaultErrorHandlers
import com.papsign.ktor.openapigen.route.apiRouting
import io.github.huiibuh.api.audiobooks.registerAudiobookRouting
import io.github.huiibuh.api.images.registerImageRouting
import io.github.huiibuh.api.metadata.registerMetadataRouting
import io.github.huiibuh.api.search.registerSearchRouting
import io.github.huiibuh.api.stream.registerStreamingRouting
import io.github.huiibuh.db.DatabaseFactory
import io.github.huiibuh.logging.disableJAudioTaggerLogs
import io.github.huiibuh.plugins.configureHTTP
import io.github.huiibuh.plugins.configureKoin
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
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


@OptIn(DelicateCoroutinesApi::class)
fun main(): Unit = runBlocking {
    disableJAudioTaggerLogs()

    launch {
        embeddedServer(Netty, port = getPort(), host = "0.0.0.0") {
            webServer()
            DatabaseFactory.connect()
            DatabaseFactory.migrate()
            launch { Scanner.rescan() }
        }.start(wait = true)
    }
}


fun Application.webServer() {
    configureKoin()
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
