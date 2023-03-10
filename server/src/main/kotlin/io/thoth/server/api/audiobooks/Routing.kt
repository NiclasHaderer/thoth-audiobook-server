package io.thoth.server.api.audiobooks

import io.ktor.server.routing.*
import io.thoth.server.api.audiobooks.library.authors.registerAuthorRouting
import io.thoth.server.api.audiobooks.library.series.registerSeriesRouting
import io.thoth.server.api.audiobooks.rescan.registerRescan

fun Route.registerAudiobookRouting() {
    route("audiobooks") {
        registerAuthorRouting()
        registerSeriesRouting()
        registerRescan()
    }
}
