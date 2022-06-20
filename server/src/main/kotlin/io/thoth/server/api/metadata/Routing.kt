package io.thoth.server.api.metadata

import io.ktor.http.*
import io.ktor.server.routing.*
import io.thoth.metadata.AuthorMetadata
import io.thoth.metadata.BookMetadata
import io.thoth.metadata.MetadataProvider
import io.thoth.metadata.SearchBookMetadata
import io.thoth.metadata.SeriesMetadata
import io.thoth.openapi.routing.get
import io.thoth.openapi.serverError
import org.koin.ktor.ext.inject

fun Route.registerMetadataRouting(path: String = "metadata") {
    route(path) {
        routing()
    }
}


internal fun Route.routing() {
    val searchService by inject<MetadataProvider>()

    get<MetadataSearch, List<SearchBookMetadata>>("search") { params ->
        searchService.search(
            params.keywords, params.title, params.author, params.narrator, params.language, params.pageSize
        )
    }

    route("author") {
        get<AuthorID, AuthorMetadata> { id ->
            searchService.getAuthorByID(id) ?: serverError(
                HttpStatusCode.NotFound, "Author with id ${id.itemID} and provider ${id.provider}was not found"
            )
        }

        get<AuthorName, List<AuthorMetadata>>("author") { params ->
            searchService.getAuthorByName(params.name)
        }
    }

    route("book") {
        get<BookID, BookMetadata> { id ->
            searchService.getBookByID(id) ?: serverError(
                HttpStatusCode.NotFound, "Book with id ${id.itemID} and provider ${id.provider}was not found"
            )
        }

        get<BookName, List<BookMetadata>>("search") { params ->
            searchService.getBookByName(params.name, params.authorName)
        }
    }

    route("series") {
        get<SeriesID, SeriesMetadata> { id ->
            searchService.getSeriesByID(id) ?: serverError(
                HttpStatusCode.NotFound, "Series with id ${id.itemID} and provider ${id.provider}was not found"
            )
        }
        get<SeriesName, List<SeriesMetadata>>("search") { params ->
            searchService.getSeriesByName(params.name, params.authorName)
        }
    }


}
