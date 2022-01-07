package io.github.huiibuh.api.audible

import api.exceptions.APINotFound
import api.exceptions.withNotFoundHandling
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import io.github.huiibuh.api.ApiTags
import io.github.huiibuh.extensions.inject
import io.github.huiibuh.metadata.AuthorMetadata
import io.github.huiibuh.metadata.MetadataProvider
import io.github.huiibuh.metadata.SearchResultMetadata
import io.github.huiibuh.metadata.SeriesMetadata

fun NormalOpenAPIRoute.registerAudibleRouting(path: String = "metadata") {
    tag(ApiTags.Audible) {
        route(path) {
            routing()
        }
    }
}


internal fun NormalOpenAPIRoute.routing() {
    val searchService: MetadataProvider by inject()

    route("search").get<AudibleSearch, List<SearchResultMetadata>>(
        info("Search for audiobooks")
    ) { params ->
        respond(searchService.search(params.keywords,
                                     params.title,
                                     params.author,
                                     params.narrator,
                                     params.language,
                                     params.pageSize))
    }
    withNotFoundHandling {
        route("author").get<AuthorID, AuthorMetadata> { id ->
            val author = searchService.getAuthorByID(id)
                ?: throw APINotFound("Author with ${id.itemID} by provider ${id.provider} was not found")

            respond(author)
        }
        route("author").get<AuthorID, AuthorMetadata> { id ->
            val author = searchService.getAuthorByID(id)
                ?: throw APINotFound("Author with ${id.itemID} by provider ${id.provider} was not found")

            respond(author)
        }
        route("series").get<SeriesID, SeriesMetadata> { id ->
            val series = searchService.getSeriesByID(id)
                ?: throw APINotFound("Series with ${id.itemID} by provider ${id.provider} was not found")
            respond(series)
        }
        // TODO
        //        route("series/name").get<SeriesID, SeriesMetadata> { id ->
        //        }
        //        route("book/title").get<BookID, BookMetadata> { id ->
        //        }
        //        route("author/name").get<AuthorID, AuthorMetadata> { id ->
        //        }
    }
}
