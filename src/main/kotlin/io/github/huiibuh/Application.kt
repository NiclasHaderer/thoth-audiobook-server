package io.github.huiibuh

import api.exceptions.withDefaultErrorHandlers
import com.papsign.ktor.openapigen.route.apiRouting
import io.github.huiibuh.api.audible.registerAudibleRouting
import io.github.huiibuh.api.audiobooks.registerAudiobookRouting
import io.github.huiibuh.api.images.registerImageRouting
import io.github.huiibuh.api.search.registerSearchRouting
import io.github.huiibuh.api.stream.registerStreamingRouting
import io.github.huiibuh.config.Settings
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
    DatabaseFactory.connectAndMigrate()
    launch { Scanner.rescan() }

    launch {
        embeddedServer(Netty, port = Settings.webUiPort, host = "0.0.0.0") {
            webServer()
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
            registerAudibleRouting()
            registerAudiobookRouting()
            registerSearchRouting()
            registerStreamingRouting()
            registerImageRouting()
        }
    }
}
