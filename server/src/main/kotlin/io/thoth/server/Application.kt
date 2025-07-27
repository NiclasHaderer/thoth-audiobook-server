package io.thoth.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.thoth.openapi.client.kotlin.KtErrorHandling
import io.thoth.openapi.client.kotlin.generateKotlinClient
import io.thoth.openapi.client.typescript.generateTsClient
import io.thoth.openapi.ktor.errors.configureStatusPages
import io.thoth.server.api.audioRouting
import io.thoth.server.api.authRoutes
import io.thoth.server.api.authorRouting
import io.thoth.server.api.bookRouting
import io.thoth.server.api.fileSystemRouting
import io.thoth.server.api.imageRouting
import io.thoth.server.api.libraryRouting
import io.thoth.server.api.metadataRouting
import io.thoth.server.api.metadataScannerRouting
import io.thoth.server.api.pingRouting
import io.thoth.server.api.scannerRouting
import io.thoth.server.api.seriesRouting
import io.thoth.server.common.extensions.get
import io.thoth.server.common.scheduling.Scheduler
import io.thoth.server.config.ThothConfig
import io.thoth.server.database.connectToDatabase
import io.thoth.server.database.migrateDatabase
import io.thoth.server.di.setupDependencyInjection
import io.thoth.server.file.scanner.FileTreeWatcher
import io.thoth.server.plugins.auth.configureAuthentication
import io.thoth.server.plugins.configureMonitoring
import io.thoth.server.plugins.configureOpenApi
import io.thoth.server.plugins.configurePartialContent
import io.thoth.server.plugins.configureRouting
import io.thoth.server.plugins.configureSerialization
import io.thoth.server.plugins.configureSockets
import io.thoth.server.repositories.LibraryRepository
import io.thoth.server.schedules.ThothSchedules
import java.util.logging.LogManager
import kotlinx.coroutines.launch
import org.slf4j.bridge.SLF4JBridgeHandler

fun main() {
    // Force every library using the standard java logger force it to use SLF4J
    LogManager.getLogManager().reset()
    SLF4JBridgeHandler.install()

    // Start the server
    embeddedServer(
            Netty,
            port = 8080,
            watchPaths = emptyList(),
            host = "0.0.0.0",
            module = Application::applicationModule,
        )
        .start(wait = true)
}

fun Application.applicationModule() {
    // Setup DI with Koin
    setupDependencyInjection()

    plugins()

    // Connect to database
    val config = get<ThothConfig>()
    connectToDatabase(config.database)
    migrateDatabase(config.database)

    routing()

    startBackgroundJobs()
}

fun Application.plugins() {
    configureStatusPages()
    configureRouting()
    configureSerialization()
    configureOpenApi()
    configurePartialContent()
    configureSockets()
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
        metadataScannerRouting()

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
    launch { get<Scheduler>().start() }
    launch {
        val scheduler = get<Scheduler>()
        val thothSchedules = get<ThothSchedules>()
        scheduler.schedule(thothSchedules.fullScan)
        scheduler.schedule(thothSchedules.retrieveMetadata)

        scheduler.register(thothSchedules.scanLibrary)

        scheduler.launchScheduledJob(thothSchedules.fullScan)
        scheduler.launchScheduledJob(thothSchedules.retrieveMetadata)
    }

    // Generate clients
    launch {
        val config = get<ThothConfig>()
        if (!config.production) {
            log.info("Generating clients")
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

    launch {
        val libraryRepository = get<LibraryRepository>()
        get<FileTreeWatcher>().watch(libraryRepository.allFolders())
    }
}
