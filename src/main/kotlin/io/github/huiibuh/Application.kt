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
import io.github.huiibuh.db.tables.Author
import io.github.huiibuh.logging.disableJAudioTaggerLogs
import io.github.huiibuh.plugins.configureHTTP
import io.github.huiibuh.plugins.configureMonitoring
import io.github.huiibuh.plugins.configureOpenAPI
import io.github.huiibuh.plugins.configurePartialContent
import io.github.huiibuh.plugins.configureRouting
import io.github.huiibuh.plugins.configureSerialization
import io.github.huiibuh.plugins.configureSockets
import io.github.huiibuh.services.Database
import io.github.huiibuh.ws.registerUpdateRoutes
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.transactions.transaction


@OptIn(DelicateCoroutinesApi::class)
fun main() {
    disableJAudioTaggerLogs()
    DatabaseFactory.connectAndMigrate()

    GlobalScope.launch {
        Database.rescan()
    }

    embeddedServer(Netty, port = Settings.webUiPort, host = "0.0.0.0") {
        webServer()
    }.start(wait = true)
}


fun Application.webServer() {
    configureOpenAPI()
    configureRouting()
    configurePartialContent()
    configureHTTP()
    configureSockets()
    configureMonitoring()
    configureSerialization()
    routing {
        get("/updateMe") {
            val author = transaction {
                val author = Author.all().first()
                author.asin = "${(0..10000000).random()}"
                author
            }
            call.respond(author.toModel())
        }
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
