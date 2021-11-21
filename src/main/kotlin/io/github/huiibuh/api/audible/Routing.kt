package io.github.huiibuh.api.audible

import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import io.github.huiibuh.api.ApiTags
import api.exceptions.withNotFoundHandling
import audible.models.AudibleAuthor
import audible.models.AudibleBook
import audible.models.AudibleSearchResult
import audible.models.AudibleSeries

fun NormalOpenAPIRoute.registerAudibleRouting(path: String = "audible") {
    tag(ApiTags.Audible) {
        route(path) {
            audibleRouting()
        }
    }
}


internal fun NormalOpenAPIRoute.audibleRouting() {
    route("search").get<AudibleSearch, List<AudibleSearchResult>>(info("Search for audiobooks")) {
        search(it)
    }

    withNotFoundHandling {
        route("author").get<AuthorASIN, AudibleAuthor> {
            getAuthor(it)
        }
        route("series").get<SeriesASIN, AudibleSeries> {
            getSeries(it)
        }
        route("book").get<AudiobookASIN, AudibleBook> {
            getBook(it)
        }
    }
}
