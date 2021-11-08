package io.github.huiibuh.api.audible

import com.papsign.ktor.openapigen.route.*
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import io.github.huiibuh.api.ApiTags
import io.github.huiibuh.audible.client.AudibleClient
import io.github.huiibuh.audible.client.AudibleError
import io.github.huiibuh.audible.client.AudibleException
import io.github.huiibuh.audible.client.AudibleNotFoundException
import io.github.huiibuh.audible.models.AudibleAuthor
import io.github.huiibuh.audible.models.AudibleBook
import io.github.huiibuh.audible.models.AudibleSearchResult
import io.github.huiibuh.audible.models.AudibleSeries
import io.github.huiibuh.services.AudibleService
import io.ktor.application.*
import io.ktor.http.*

fun Application.registerAudibleRouting(path: String = "audible") {
    val audibleService = AudibleService


    apiRouting {
        throws(HttpStatusCode.NotFound,
               AudibleError(404, "Not found"),
               { ex: AudibleNotFoundException -> ex.toModel() }) {

            throws(HttpStatusCode.BadGateway,
                   AudibleError(400, "Something went bad"),
                   { ex: AudibleException -> ex.toModel() }) {

                tag(ApiTags.Audible) {
                    route(path) {
                        audibleRouting(audibleService)
                    }
                }

            }
        }
    }
}


fun NormalOpenAPIRoute.audibleRouting(audibleService: AudibleClient) {
    route("search").get<AudibleSearch, List<AudibleSearchResult>>(
        info("Search for audiobooks")
    ) { query ->
        val response = audibleService.search(query.keywords,
                                             query.title,
                                             query.author,
                                             query.narrator,
                                             query.language,
                                             query.pageSize)
        respond(response)
    }
    route("author").get<AuthorASIN, AudibleAuthor> { author ->

        val response = audibleService.getAuthorInfo(author.asin)
        respond(response)
    }
    route("series").get<SeriesASIN, AudibleSeries> { series ->
        val response = audibleService.getSeriesInfo(series.asin)
        respond(response)
    }
    route("book").get<AudiobookASIN, AudibleBook> { book ->
        val response = audibleService.getBookInfo(book.asin)
        respond(response)
    }
}
