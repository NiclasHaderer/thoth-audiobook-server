package io.thoth.server.api.metadata

import io.ktor.http.*
import io.ktor.server.routing.*
import io.thoth.metadata.MetadataProvider
import io.thoth.metadata.responses.MetadataAuthor
import io.thoth.metadata.responses.MetadataBook
import io.thoth.metadata.responses.MetadataSearchBook
import io.thoth.metadata.responses.MetadataSeries
import io.thoth.openapi.routing.get
import io.thoth.openapi.serverError
import org.koin.ktor.ext.inject

fun Route.registerMetadataRouting() =
    route("metadata") {
        val metadataProvider by inject<MetadataProvider>()

        get<MetadataSearch, List<MetadataSearchBook>>("search") { params ->
            metadataProvider.search(
                params.keywords,
                params.title,
                params.author,
                params.narrator,
                params.language,
                params.pageSize
            )
        }

        route("author") {
            get<AuthorID, MetadataAuthor> { id ->
                metadataProvider.getAuthorByID(id.provider, id.itemID)
                    ?: serverError(
                        HttpStatusCode.NotFound,
                        "Author with id ${id.itemID} and provider ${id.provider}was not found"
                    )
            }

            get<AuthorName, List<MetadataAuthor>>("search") { params ->
                metadataProvider.getAuthorByName(params.name)
            }
        }

        route("book") {
            get<BookID, MetadataBook> { id ->
                metadataProvider.getBookByID(id.provider, id.itemID)
                    ?: serverError(
                        HttpStatusCode.NotFound,
                        "Book with id ${id.itemID} and provider ${id.provider}was not found"
                    )
            }

            get<BookName, List<MetadataBook>>("search") { params ->
                metadataProvider.getBookByName(params.name, params.authorName)
            }
        }

        route("series") {
            get<SeriesID, MetadataSeries> { id ->
                metadataProvider.getSeriesByID(id.provider, id.itemID)
                    ?: serverError(
                        HttpStatusCode.NotFound,
                        "Series with id ${id.itemID} and provider ${id.provider}was not found"
                    )
            }
            get<SeriesName, List<MetadataSeries>>("search") { params ->
                metadataProvider.getSeriesByName(params.name, params.authorName)
            }
        }
    }
