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
import io.github.huiibuh.metadata.AuthorMetadata
import io.github.huiibuh.metadata.BookMetadata
import io.github.huiibuh.metadata.MetadataProvider
import io.github.huiibuh.metadata.SearchResultMetadata
import io.github.huiibuh.metadata.SeriesMetadata

fun NormalOpenAPIRoute.registerMetadataRouting(path: String = "metadata") {
    tag(ApiTags.Metadata) {
        route(path) {
            routing()
        }
    }
}


internal fun NormalOpenAPIRoute.routing() {
    val searchService: MetadataProvider by inject()

    route("search").get<MetadataSearch, List<SearchResultMetadata>>(
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
        route("book").get<BookID, BookMetadata> { id ->
            val book = searchService.getBookByID(id)
                ?: throw APINotFound("Author with ${id.itemID} by provider ${id.provider} was not found")

            respond(book)
        }
        route("series").get<SeriesID, SeriesMetadata> { id ->
            val series = searchService.getSeriesByID(id)
                ?: throw APINotFound("Series with ${id.itemID} by provider ${id.provider} was not found")
            respond(series)
        }
        route("series/search/").get<SeriesName, SeriesMetadata> { name ->
            val author = searchService.getSeriesByName(name.name)
                ?: throw APINotFound("Series with name ${name.name} was not found")
            respond(author)
        }
        route("book/title").get<AuthorName, BookMetadata> { name ->
            val author = searchService.getBookByName(name.name)
                ?: throw APINotFound("Book with name ${name.name} was not found")
            respond(author)
        }
        route("author/name").get<BookName, AuthorMetadata> { name ->
            val author = searchService.getAuthorByName(name.name)
                ?: throw APINotFound("Author with name ${name.name} was not found")
            respond(author)
        }
    }
}
