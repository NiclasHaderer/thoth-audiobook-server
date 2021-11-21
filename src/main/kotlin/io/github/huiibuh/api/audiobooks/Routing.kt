package io.github.huiibuh.api.audiobooks

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import io.github.huiibuh.api.ApiTags
import io.github.huiibuh.api.audiobooks.books.bookRouting
import io.github.huiibuh.api.audiobooks.authors.authorRouting
import io.github.huiibuh.api.audiobooks.series.seriesRouting

fun NormalOpenAPIRoute.registerAudiobookRouting(route: String = "audiobooks") {
    route(route) {
        tag(ApiTags.Books) {
            bookRouting()
        }
        tag(ApiTags.Authors) {
            authorRouting()
        }

        tag(ApiTags.Series) {
            seriesRouting()
        }
    }
}
