package io.thoth.server

import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.http.HttpStatusCode
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.thoth.openapi.client.kotlin.KtErrorHandling
import io.thoth.openapi.client.kotlin.generateKotlinClient
import io.thoth.openapi.client.typescript.generateTsClient
import io.thoth.openapi.ktor.errors.ErrorStatuses
import io.thoth.openapi.ktor.errors.configureStatusPages
import io.thoth.metadata.audible.client.AudibleUnavailableException
import io.thoth.server.api.audioRouting
import io.thoth.server.api.authRoutes
import io.thoth.server.api.authorRouting
import io.thoth.server.api.bookRouting
import io.thoth.server.api.fileSystemRouting
import io.thoth.server.api.imageRouting
import io.thoth.server.api.libraryRouting
import io.thoth.server.api.metadataRouting
import io.thoth.server.api.metadataAgentRouting
import io.thoth.server.api.pingRouting
import io.thoth.server.api.scannerRouting
import io.thoth.server.api.seriesRouting
import io.thoth.server.common.scheduling.Scheduler
import io.thoth.server.config.ThothConfig
import io.thoth.server.database.DatabaseConnector
import io.thoth.server.di.setupDependencyInjection
import io.thoth.server.plugins.auth.configureAuthentication
import io.thoth.server.plugins.configureMonitoring
import io.thoth.server.plugins.configureOpenApi
import io.thoth.server.plugins.configurePartialContent
import io.thoth.server.plugins.configureRouting
import io.thoth.server.plugins.configureSerialization
import io.thoth.server.plugins.configureSockets
import io.thoth.server.schedules.ThothSchedules
import kotlinx.coroutines.launch
import org.koin.ktor.ext.get
import org.slf4j.bridge.SLF4JBridgeHandler
import java.util.logging.LogManager

fun main() {
    // Force every library using the standard java logger to use SLF4J
    LogManager.getLogManager().reset()
    SLF4JBridgeHandler.install()

    // Loaded before the server starts so a broken config fails immediately instead of on first injection
    val config = ThothConfig.load()

    // Start the server
    embeddedServer(
        Netty,
        port = config.port,
        watchPaths = emptyList(),
        host = "0.0.0.0",
        module = { applicationModule(config) },
    ).start(wait = true)
}

fun Application.applicationModule(config: ThothConfig) {
    setupDependencyInjection(config)
    DatabaseConnector.connect()
    plugins()
    routing()
    startBackgroundJobs()
}


fun Application.plugins() {
    configureStatusPages {
        status<AudibleUnavailableException>(HttpStatusCode.BadGateway)
    }
    configureRouting()
    val mapper = configureSerialization()
    configureOpenApi()
    configurePartialContent()
    configureSockets(mapper)
    configureMonitoring()
    configureAuthentication()
}

fun Application.routing() {
    routing {
        // Authentication
        authRoutes()

        // List directories
        fileSystemRouting()

        // Libraries and their resources
        libraryRouting()
        scannerRouting()
        metadataAgentRouting()

        // Library resources
        bookRouting()
        seriesRouting()
        authorRouting()

        // Metadata for the resources
        metadataRouting()

        // Static files
        audioRouting()
        imageRouting()

        // Routes for checking if the server is available
        pingRouting()
    }
}

fun Application.startBackgroundJobs() {
    val scheduler = get<Scheduler>()
    val thothSchedules = get<ThothSchedules>()
    scheduler.schedule(thothSchedules.fullScan)
    scheduler.launchNow(thothSchedules.fullScan)
    launch { scheduler.start() }

    // Generate clients
    if (developmentMode) {
        launch {
            log.info("Generating clients")
            // TODO generateTsClient("../thoth-web/src/client/generated/client/typescript")
            generateTsClient("gen/client/typescript")
            generateKotlinClient(
                apiClientPackageName = "io.thoth.client.gen",
                savePath = "client/src/main/kotlin/io/thoth/client/gen",
                apiClientName = "ThothClient",
                errorHandling = KtErrorHandling.Either,
            )
            log.info("Clients generated")
        }
    }
}
