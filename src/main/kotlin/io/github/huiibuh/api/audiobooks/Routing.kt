package io.github.huiibuh.api.audiobooks

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.route
import io.github.huiibuh.api.audiobooks.authors.registerAuthorRouting
import io.github.huiibuh.api.audiobooks.books.registerBookRouting
import io.github.huiibuh.api.audiobooks.rescan.registerRescan
import io.github.huiibuh.api.audiobooks.series.registerSeriesRouting

fun NormalOpenAPIRoute.registerAudiobookRouting(route: String = "audiobooks") {
    route(route) {
        registerBookRouting()
        registerAuthorRouting()
        registerSeriesRouting()
        registerRescan()
    }
}
