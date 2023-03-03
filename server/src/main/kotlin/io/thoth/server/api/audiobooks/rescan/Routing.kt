package io.thoth.server.api.audiobooks.rescan

import io.ktor.server.routing.*
import io.thoth.openapi.routing.post
import io.thoth.server.file.scanner.fullScan
import kotlinx.coroutines.launch

fun Route.registerRescan(path: String = "rescan") =
    route(path) {
        // TODO inject the scanner with DI
        // TODO add a way to stop the scanner
        post<Unit, Unit> {
            launch {
                // TODO scan only certain library
                fullScan()
            }
        }
    }
