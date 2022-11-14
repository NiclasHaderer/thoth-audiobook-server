package io.thoth.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.thoth.auth.configureAuthentication
import io.thoth.common.extensions.get
import io.thoth.common.extensions.shutdown
import io.thoth.database.tables.meta.MetaBook
import io.thoth.database.tables.meta.MetaGenre
import io.thoth.database.tables.meta.TMetaGenreBookMapping
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
import io.thoth.server.file.persister.withAutomaticMetadata
import io.thoth.server.file.scanner.FileWatcher
import io.thoth.server.file.scanner.RecursiveScan
import io.thoth.server.koin.configureKoin
import io.thoth.server.logging.disableJAudioTaggerLogs
import io.thoth.server.plugins.*
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction


fun main() {
    disableJAudioTaggerLogs()
    val config = loadConfig()
    embeddedServer(
        Netty, port = config.port, watchPaths = listOf("classes"), host = "0.0.0.0"
    ) {
        configureKoin(config)

        try {
            connectToDatabase().also {
                migrateDatabase()
            }
            transaction {
                val book = MetaBook.new {
                    title = "eragon"
                    provider = "audible"
                    itemID = "eragon-1"
                }

                val book2 = MetaBook.new {
                    title = "harry potter"
                    provider = "audible"
                    itemID = "potter-1"
                }

                val genre = MetaGenre.new {
                    name = "fantasy"
                }

                val genre2 = MetaGenre.new {
                    name = "action"
                }

                TMetaGenreBookMapping.insert {
                    it[this.genre] = genre.id
                    it[this.book] = book.id
                }

                TMetaGenreBookMapping.insert {
                    it[this.genre] = genre.id
                    it[this.book] = book2.id
                }

                TMetaGenreBookMapping.insert {
                    it[this.genre] = genre2.id
                    it[this.book] = book2.id
                }


            }

            run {}.also {
                withAutomaticMetadata()
            }.also {
                launch {
                    get<FileWatcher>().watch()
                }
            }.also {
                launch {
                    RecursiveScan().start()
                }
            }
            webServer(config)
        } catch (e: Exception) {
            log.error("Could not start server", e)
            shutdown()
        }

    }.start(wait = true)
}

fun Application.webServer(config: ThothConfig) {
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

