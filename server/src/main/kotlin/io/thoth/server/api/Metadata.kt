package io.thoth.server.api

import io.ktor.http.*
import io.ktor.server.routing.*
import io.thoth.metadata.MetadataProvider
import io.thoth.metadata.responses.MetadataAuthor
import io.thoth.metadata.responses.MetadataBook
import io.thoth.metadata.responses.MetadataSearchBook
import io.thoth.metadata.responses.MetadataSeries
import io.thoth.openapi.get
import io.thoth.openapi.serverError
import org.koin.ktor.ext.inject

fun Routing.metadataRouting() {
    val metadataProvider by inject<MetadataProvider>()

    get<Api.Metadata.Search, List<MetadataSearchBook>> { params ->
        metadataProvider.search(
            params.keywords,
            params.title,
            params.author,
            params.narrator,
            params.language,
            params.pageSize,
        )
    }

    get<Api.Metadata.Author.Id, MetadataAuthor> { id ->
        metadataProvider.getAuthorByID(id.provider, id.id)
            ?: serverError(
                HttpStatusCode.NotFound,
                "Author with id ${id.id} and provider ${id.provider}was not found",
            )
    }

    get<Api.Metadata.Author.Search, List<MetadataAuthor>> { params -> metadataProvider.getAuthorByName(params.q) }

    get<Api.Metadata.Book.Id, MetadataBook> { id ->
        metadataProvider.getBookByID(id.provider, id.id)
            ?: serverError(
                HttpStatusCode.NotFound,
                "Book with id ${id.id} and provider ${id.provider}was not found",
            )
    }

    get<Api.Metadata.Book.Search, List<MetadataBook>> { params ->
        metadataProvider.getBookByName(params.q, params.authorName)
    }

    get<Api.Metadata.Series.Id, MetadataSeries> { id ->
        metadataProvider.getSeriesByID(id.provider, id.id)
            ?: serverError(
                HttpStatusCode.NotFound,
                "Series with id ${id.id} and provider ${id.provider}was not found",
            )
    }
    get<Api.Metadata.Series.Search, List<MetadataSeries>> { params ->
        metadataProvider.getSeriesByName(params.q, params.authorName)
    }
}
