package io.thoth.server.api

import io.ktor.server.routing.*
import io.thoth.generators.openapi.errors.ErrorResponse
import io.thoth.generators.openapi.get
import io.thoth.metadata.MetadataProvider
import io.thoth.metadata.responses.MetadataAuthor
import io.thoth.metadata.responses.MetadataBook
import io.thoth.metadata.responses.MetadataSearchBook
import io.thoth.metadata.responses.MetadataSeries
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

    get<Api.Metadata.Author.Id, MetadataAuthor> {
        metadataProvider.getAuthorByID(it.provider, it.id)
            ?: throw ErrorResponse.notFound(
                "Author",
                it.id,
                "Provider ${it.provider}",
            )
    }

    get<Api.Metadata.Author.Search, List<MetadataAuthor>> { params -> metadataProvider.getAuthorByName(params.q) }

    get<Api.Metadata.Book.Id, MetadataBook> { id ->
        metadataProvider.getBookByID(id.provider, id.id)
            ?: throw ErrorResponse.notFound(
                "Book",
                id.id,
                "Provider ${id.provider}",
            )
    }

    get<Api.Metadata.Book.Search, List<MetadataBook>> { params ->
        metadataProvider.getBookByName(params.q, params.authorName)
    }

    get<Api.Metadata.Series.Id, MetadataSeries> { id ->
        metadataProvider.getSeriesByID(id.provider, id.id)
            ?: throw ErrorResponse.notFound(
                "Series",
                id.id,
                "Provider ${id.provider}",
            )
    }
    get<Api.Metadata.Series.Search, List<MetadataSeries>> { params ->
        metadataProvider.getSeriesByName(params.q, params.authorName)
    }
}
