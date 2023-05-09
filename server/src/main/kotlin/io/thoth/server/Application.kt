package io.thoth.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.thoth.generators.openapi.OpenApiRouteCollector
import io.thoth.generators.openapi.errors.configureStatusPages
import io.thoth.generators.typescript.TsClientCreator
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
import io.thoth.server.plugins.authentication.configureAuthentication
import io.thoth.server.plugins.configureMonitoring
import io.thoth.server.plugins.configureOpenApi
import io.thoth.server.plugins.configurePartialContent
import io.thoth.server.plugins.configureRouting
import io.thoth.server.plugins.configureSerialization
import io.thoth.server.plugins.configureSockets
import io.thoth.server.schedules.ThothSchedules
import io.thoth.server.services.LibraryRepository
import java.io.File
import java.util.logging.LogManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.bridge.SLF4JBridgeHandler

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
            port = 8080,
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
        val libraryRepository = get<LibraryRepository>()
        get<FileTreeWatcher>().watch(libraryRepository.allFolders())
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
    configureSockets()
    configureMonitoring()
    configureSerialization()
    configureAuthentication()

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
    if (!config.production) {
        launch {
            TsClientCreator(
                    OpenApiRouteCollector.values(),
                    File("models.ts"),
                    File("client.ts"),
                )
                .also {
                    it.saveClient()
                    it.saveTypes()
                }
        }
    }
}
