package io.thoth.server.api

import io.ktor.server.routing.Routing
import io.thoth.metadata.MetadataAgent
import io.thoth.metadata.MetadataAgents
import io.thoth.metadata.responses.MetadataAuthor
import io.thoth.metadata.responses.MetadataBook
import io.thoth.metadata.responses.MetadataSearchBook
import io.thoth.metadata.responses.MetadataSeries
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.openapi.ktor.get
import io.thoth.server.repositories.LibraryRepository
import kotlinx.coroutines.flow.toList
import org.koin.ktor.ext.inject
import java.util.UUID

fun Routing.metadataRouting() {
    val metadataAgents by inject<MetadataAgents>()
    val libraryRepository by inject<LibraryRepository>()

    fun agentFor(libraryId: UUID): Pair<MetadataAgent, String> =
        libraryRepository.raw(libraryId).let { metadataAgents.forLibrary(it) to it.language }

    get<Api.Libraries.Id.Metadata.Search, List<MetadataSearchBook>> {
        val (metadataAgent, region) = agentFor(it.libraryId)

        metadataAgent.search(
            region = region,
            keywords = it.keywords,
            title = it.title,
            author = it.author,
            narrator = it.narrator,
            language = it.language,
            pageSize = it.pageSize,
        )
    }

    get<Api.Libraries.Id.Metadata.Author.Id, MetadataAuthor> {
        val (metadataAgent, region) = agentFor(it.libraryId)
        metadataAgent.getAuthorByID(providerId = it.provider, authorId = it.id, region = region)
            ?: throw ErrorResponse.notFound("Author", it.id, "Provider ${it.provider}")
    }

    get<Api.Libraries.Id.Metadata.Author.Search, List<MetadataAuthor>> {
        val (metadataAgent, region) = agentFor(it.libraryId)
        metadataAgent.getAuthorByName(authorName = it.q, region = region).toList()
    }

    get<Api.Libraries.Id.Metadata.Book.Id, MetadataBook> {
        val (metadataAgent, region) = agentFor(it.libraryId)
        metadataAgent.getBookByID(providerId = it.provider, region = region, bookId = it.id)
            ?: throw ErrorResponse.notFound("Book", it.id, "Provider ${it.provider}")
    }

    get<Api.Libraries.Id.Metadata.Book.Search, List<MetadataBook>> {
        val (metadataAgent, region) = agentFor(it.libraryId)
        metadataAgent.getBookByName(bookName = it.q, region = region, authorName = it.authorName).toList()
    }

    get<Api.Libraries.Id.Metadata.Series.Id, MetadataSeries> {
        val (metadataAgent, region) = agentFor(it.libraryId)
        metadataAgent.getSeriesByID(providerId = it.provider, region = region, seriesId = it.id)
            ?: throw ErrorResponse.notFound("Series", it.id, "Provider ${it.provider}")
    }
    get<Api.Libraries.Id.Metadata.Series.Search, List<MetadataSeries>> {
        val (metadataAgent, region) = agentFor(it.libraryId)
        metadataAgent.getSeriesByName(seriesName = it.q, region = region, authorName = it.authorName).toList()
    }
}
