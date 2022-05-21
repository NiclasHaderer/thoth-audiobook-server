package io.thoth.server.plugins

import io.ktor.application.*
import io.ktor.features.*


fun Application.configurePartialContent() {
    install(PartialContent) {
        maxRangeCount = 10
    }
}