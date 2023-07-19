package io.thoth.server.api

import io.ktor.server.routing.*
import io.thoth.metadata.MetadataProvider
import io.thoth.metadata.responses.MetadataAuthor
import io.thoth.metadata.responses.MetadataBook
import io.thoth.metadata.responses.MetadataSearchBook
import io.thoth.metadata.responses.MetadataSeries
import io.thoth.openapi.openapi.errors.ErrorResponse
import io.thoth.openapi.openapi.get
import org.koin.ktor.ext.inject

fun Routing.metadataRouting() {
    val metadataProvider by inject<MetadataProvider>()

    get<Api.Metadata.Search, List<MetadataSearchBook>> { params ->
        metadataProvider.search(
            region = params.region,
            keywords = params.keywords,
            title = params.title,
            author = params.author,
            narrator = params.narrator,
            language = params.language,
            pageSize = params.pageSize,
        )
    }

    get<Api.Metadata.Author.Id, MetadataAuthor> {
        metadataProvider.getAuthorByID(providerId = it.provider, authorId = it.id, region = it.region)
            ?: throw ErrorResponse.notFound(
                "Author",
                it.id,
                "Provider ${it.provider}",
            )
    }

    get<Api.Metadata.Author.Search, List<MetadataAuthor>> {
        metadataProvider.getAuthorByName(
            authorName = it.q,
            region = it.region,
        )
    }

    get<Api.Metadata.Book.Id, MetadataBook> {
        metadataProvider.getBookByID(providerId = it.provider, region = it.region, bookId = it.id)
            ?: throw ErrorResponse.notFound(
                "Book",
                it.id,
                "Provider ${it.provider}",
            )
    }

    get<Api.Metadata.Book.Search, List<MetadataBook>> {
        metadataProvider.getBookByName(bookName = it.q, region = it.region, authorName = it.authorName)
    }

    get<Api.Metadata.Series.Id, MetadataSeries> {
        metadataProvider.getSeriesByID(providerId = it.provider, region = it.region, seriesId = it.id)
            ?: throw ErrorResponse.notFound(
                "Series",
                it.id,
                "Provider ${it.provider}",
            )
    }
    get<Api.Metadata.Series.Search, List<MetadataSeries>> {
        metadataProvider.getSeriesByName(seriesName = it.q, region = it.region, authorName = it.authorName)
    }
}
