package io.github.huiibuh

import io.github.huiibuh.api.audible.registerAudibleRouting
import io.github.huiibuh.api.audiobooks.registerAudiobookRouting
import io.github.huiibuh.db.connectToDatabase
import io.github.huiibuh.plugins.*
import io.github.huiibuh.scanner.traverseAudioFiles
import io.ktor.server.engine.*
import io.ktor.server.netty.*


fun main() {
    connectToDatabase()
    traverseAudioFiles("/home/niclas/Desktop/audio", {path, basicFileAttributes ->  })
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureOpenAPI()
        configureRouting()
        configureHTTP()
        configureMonitoring()
        configureSerialization()
        registerAudibleRouting()
        registerAudiobookRouting()
    }.start(wait = true)
}
