package io.thoth.server.api.audiobooks.rescan

import io.ktor.server.routing.*
import io.thoth.openapi.routing.post
import io.thoth.server.file.scanner.CompleteScan

fun Route.registerRescan(path: String = "rescan") {
    route(path) {
        post<Unit, Unit> {
            CompleteScan().start()
        }
    }
}

