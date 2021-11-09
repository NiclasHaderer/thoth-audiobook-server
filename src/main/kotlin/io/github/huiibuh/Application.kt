package io.github.huiibuh

import io.github.huiibuh.db.DatabaseFactory
import io.github.huiibuh.logging.disableJAudioTaggerLogs
import io.github.huiibuh.scanner.TrackReference
import io.github.huiibuh.services.DB


fun main() {
    disableJAudioTaggerLogs()
    DatabaseFactory.connectAndMigrate()
    DB.runValidation()
    DB.importMissingTracks()

    //    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
    //        configureOpenAPI()
    //        configureRouting()
    //        configureHTTP()
    //        configureMonitoring()
    //        configureSerialization()
    //        registerAudibleRouting()
    //        registerAudiobookRouting()
    //    }.start(wait = true)
}
