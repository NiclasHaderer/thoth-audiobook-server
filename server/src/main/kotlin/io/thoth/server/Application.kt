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
import io.thoth.server.api.audiobooks.registerAudiobookRouting
import io.thoth.server.api.images.registerImageRouting
import io.thoth.server.api.metadata.registerMetadataRouting
import io.thoth.server.api.search.registerSearchRouting
import io.thoth.server.api.stream.registerStreamingRouting
import io.thoth.server.di.setupDependencyInjection
import io.thoth.server.file.scanner.FileTreeWatcher
import io.thoth.server.plugins.configureCORS
import io.thoth.server.plugins.configureMonitoring
import io.thoth.server.plugins.configureOpenApi
import io.thoth.server.plugins.configurePartialContent
import io.thoth.server.plugins.configureRouting
import io.thoth.server.plugins.configureSerialization
import io.thoth.server.plugins.configureSockets
import io.thoth.server.schedules.ThothSchedules
import java.util.logging.LogManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.transactions.transaction
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
                scheduler.schedule(thothSchedules.getMetadata)
                scheduler.launchScheduledJob(thothSchedules.fullScan)
                scheduler.launchScheduledJob(thothSchedules.getMetadata)
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

    configureStatusPages()
    configureRouting()
    configureOpenApi()
    configureAuthentication(config.configDirectory) {
        domain = "127.0.0.1:${config.port}"
        protocol = if (config.TLS) URLProtocol.HTTPS else URLProtocol.HTTP
    }
    configurePartialContent()
    configureCORS(config)
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
