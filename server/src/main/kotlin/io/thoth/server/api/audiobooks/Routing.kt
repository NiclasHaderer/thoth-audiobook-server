package io.thoth.server.api.audiobooks

import io.ktor.server.routing.*
import io.thoth.server.api.audiobooks.authors.registerAuthorRouting
import io.thoth.server.api.audiobooks.books.registerBookRouting
import io.thoth.server.api.audiobooks.rescan.registerRescan
import io.thoth.server.api.audiobooks.series.registerSeriesRouting

fun Route.registerAudiobookRouting(route: String = "audiobooks") {
    route(route) {
        registerBookRouting()
        registerAuthorRouting()
        registerSeriesRouting()
        registerRescan()
    }
}
