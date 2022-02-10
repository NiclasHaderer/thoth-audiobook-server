package io.github.huiibuh.api.metadata

import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import io.github.huiibuh.api.ApiTags
import io.github.huiibuh.api.exceptions.APINotFound
import io.github.huiibuh.api.exceptions.withNotFoundHandling
import io.github.huiibuh.extensions.inject
import io.github.huiibuh.metadata.*

fun NormalOpenAPIRoute.registerMetadataRouting(path: String = "metadata") {
    tag(ApiTags.Metadata) {
        route(path) {
            routing()
        }
    }
}


internal fun NormalOpenAPIRoute.routing() {
    val searchService: MetadataProvider by inject()

    route("search").get<MetadataSearch, List<SearchBookMetadata>>(
        info("Search for audiobooks")
    ) { params ->
        respond(
            searchService.search(
                params.keywords,
                params.title,
                params.author,
                params.narrator,
                params.language,
                params.pageSize
            )
        )
    }
    withNotFoundHandling {
        route("author").get<AuthorID, AuthorMetadata> { id ->
            val author =
                searchService.getAuthorByID(id)
                    ?: throw APINotFound("Author with id ${id.itemID} and provider ${id.provider}was not found")
            respond(author)
        }
        route("book").get<BookID, BookMetadata> { id ->
            val book = searchService.getBookByID(id)
                ?: throw APINotFound("Book with id ${id.itemID} and provider ${id.provider}was not found")
            respond(book)
        }
        route("series").get<SeriesID, SeriesMetadata> { id ->
            val series = searchService.getSeriesByID(id)
                ?: throw APINotFound("Series with id ${id.itemID} and provider ${id.provider}was not found")
            respond(series)
        }
        route("series/search/").get<SeriesName, List<SeriesMetadata>> { params ->
            val author = searchService.getSeriesByName(params.name, params.authorName)
            respond(author)
        }
        route("book/title").get<BookName, List<BookMetadata>> { params ->
            val author = searchService.getBookByName(params.name, params.authorName)
            respond(author)
        }
        route("author/name").get<AuthorName, List<AuthorMetadata>> { params ->
            val author = searchService.getAuthorByName(params.name)
            respond(author)
        }
    }
}
