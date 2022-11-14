package io.thoth.server.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.partialcontent.*


fun Application.configurePartialContent() {
    install(PartialContent) {
        maxRangeCount = 10
    }
}
