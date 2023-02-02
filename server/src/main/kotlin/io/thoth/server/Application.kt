package io.thoth.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.thoth.auth.configureAuthentication
import io.thoth.common.extensions.get
import io.thoth.common.extensions.shutdown
import io.thoth.openapi.configureStatusPages
import io.thoth.server.api.audiobooks.registerAudiobookRouting
import io.thoth.server.api.images.registerImageRouting
import io.thoth.server.api.metadata.registerMetadataRouting
import io.thoth.server.api.search.registerSearchRouting
import io.thoth.server.api.stream.registerStreamingRouting
import io.thoth.server.config.ThothConfig
import io.thoth.server.config.loadConfig
import io.thoth.server.db.connectToDatabase
import io.thoth.server.db.migrateDatabase
import io.thoth.server.file.scanner.FileWatcher
import io.thoth.server.file.scanner.RecursiveScan
import io.thoth.server.plugins.*
import kotlinx.coroutines.launch
import org.slf4j.bridge.SLF4JBridgeHandler
import java.util.logging.LogManager


fun main() {
    LogManager.getLogManager().reset()
    SLF4JBridgeHandler.install()
    embeddedServer(
        Netty,
        port = loadConfig().port,
        watchPaths = listOf("classes"),
        host = "0.0.0.0",
        module = Application::applicationModule
    ).start(wait = true)
}

fun Application.applicationModule() {
    val config = loadConfig()
    configureKoin(config)

    try {
        connectToDatabase().also {
            migrateDatabase()
        }.also {
            launch {
                get<FileWatcher>().watch()
            }
        }.also {
            launch {
                RecursiveScan().start()
            }
        }
        server(config)
    } catch (e: Exception) {
        log.error("Could not start server", e)
        shutdown()
    }
}

fun Application.server(config: ThothConfig) {
    configureStatusPages()
    configureRouting()
    configureOpenApi()
    configureAuthentication(config.configDirectory)
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
