package io.thoth.server.plugins

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.request.*
import org.slf4j.event.Level

fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.WARN
        filter { call -> call.request.path().startsWith("/") }
    }

}