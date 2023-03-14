package io.thoth.server

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.thoth.auth.configureAuthentication
import io.thoth.common.extensions.get
import io.thoth.common.scheduling.Scheduler
import io.thoth.config.ThothConfig
import io.thoth.database.access.allFolders
import io.thoth.database.connectToDatabase
import io.thoth.database.migrateDatabase
import io.thoth.database.tables.Library
import io.thoth.openapi.configureStatusPages
import io.thoth.server.api.Api
import io.thoth.server.api.audiobooks.registerAudiobookRouting
import io.thoth.server.api.v1.*
import io.thoth.server.di.setupDependencyInjection
import io.thoth.server.file.scanner.FileTreeWatcher
import io.thoth.server.plugins.*
import io.thoth.server.schedules.ThothSchedules
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.serializer
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.bridge.SLF4JBridgeHandler
import java.util.logging.LogManager

fun main() {
    // Force every library which is using the standard java logger to use SLF4J
    LogManager.getLogManager().reset()
    SLF4JBridgeHandler.install()

    // Setup DI with Koin
    setupDependencyInjection()

    // Connect to database
    val config = get<ThothConfig>()
    connectToDatabase(config.database)
    migrateDatabase(config.database)

    // Start the server
    embeddedServer(
        Netty,
        port = config.port,
        watchPaths = listOf("classes"),
        host = "0.0.0.0",
        module = Application::applicationModule,
    )
        .start(wait = true)
}

fun Application.applicationModule() {
    launch { get<Scheduler>().start() }
    runBlocking {
        launch {
            val scheduler = get<Scheduler>()
            val thothSchedules = get<ThothSchedules>()
            scheduler.register(thothSchedules.scanLibrary)
            scheduler.schedule(thothSchedules.fullScan)
            scheduler.schedule(thothSchedules.retrieveMetadata)
            scheduler.launchScheduledJob(thothSchedules.fullScan)
            scheduler.launchScheduledJob(thothSchedules.retrieveMetadata)
        }
            .join()
    }

    launch {
        val folders = transaction { Library.allFolders() }
        get<FileTreeWatcher>().watch(folders)
    }
    server()
}

fun Application.server() {
    val config = get<ThothConfig>()

    // Install plugins
    configureStatusPages()
    configureRouting()
    configureOpenApi()
    configurePartialContent()
    configureCORS(production = config.production)
    configureSockets()
    configureMonitoring()
    configureSerialization()

    // Authentication
    val authRoutes = configureAuthentication {
        domain = "127.0.0.1:${config.port}"
        protocol = if (config.TLS) URLProtocol.HTTPS else URLProtocol.HTTP
        jwksPath = "/api/.well-known/jwks.json"
        keyPairPath = "${config.configDirectory}/jwt.pem"
    }

    routing {
        authRoutes(authRoutes)
        bookRouting()
        metadataRouting()
        audioRouting()
        imageRouting()
        pingRouting()
        searchRouting()
        libraryRouting()

        route("api") {
            registerAudiobookRouting()
        }
    }
}
