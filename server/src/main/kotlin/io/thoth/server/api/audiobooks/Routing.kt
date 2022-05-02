package io.thoth.server.api.audiobooks

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.route
import io.thoth.server.api.audiobooks.authors.registerAuthorRouting
import io.thoth.server.api.audiobooks.books.registerBookRouting
import io.thoth.server.api.audiobooks.rescan.registerRescan
import io.thoth.server.api.audiobooks.series.registerSeriesRouting

fun NormalOpenAPIRoute.registerAudiobookRouting(route: String = "audiobooks") {
    route(route) {
        registerBookRouting()
        registerAuthorRouting()
        registerSeriesRouting()
        registerRescan()
    }
}
