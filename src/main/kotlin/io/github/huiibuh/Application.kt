package io.github.huiibuh

import com.papsign.ktor.openapigen.route.apiRouting
import io.github.huiibuh.api.audiobooks.registerAudiobookRouting
import io.github.huiibuh.api.exceptions.withDefaultErrorHandlers
import io.github.huiibuh.api.images.registerImageRouting
import io.github.huiibuh.api.metadata.registerMetadataRouting
import io.github.huiibuh.api.search.registerSearchRouting
import io.github.huiibuh.api.stream.registerStreamingRouting
import io.github.huiibuh.db.DatabaseFactory
import io.github.huiibuh.db.tables.TAuthors
import io.github.huiibuh.db.tables.TBooks
import io.github.huiibuh.db.tables.TSeries
import io.github.huiibuh.db.tables.TTracks
import io.github.huiibuh.db.update.interceptor.withUpdateTime
import io.github.huiibuh.di.configureKoin
import io.github.huiibuh.file.scanner.CompleteScan
import io.github.huiibuh.file.scanner.FileChangeService
import io.github.huiibuh.logging.disableJAudioTaggerLogs
import io.github.huiibuh.plugins.*
import io.github.huiibuh.settings.getPort
import io.github.huiibuh.ws.registerUpdateRoutes
import io.ktor.application.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.launch


fun main() {
    disableJAudioTaggerLogs()
    embeddedServer(Netty, port = getPort(), host = "0.0.0.0") {
        // Has to be done in here for some strange scoping reasons
        configureKoin()
        launch {
            DatabaseFactory.connect()
            DatabaseFactory.migrate()
            withUpdateTime(TAuthors, TBooks, TSeries, TTracks)
            CompleteScan().start()
            FileChangeService().watch()
        }

        webServer()
    }.start(wait = false)
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
