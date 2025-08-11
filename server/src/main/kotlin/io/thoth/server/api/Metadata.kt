package io.thoth.server.api

import io.ktor.server.routing.*
import io.thoth.metadata.MetadataProviders
import io.thoth.metadata.MetadataWrapper
import io.thoth.metadata.responses.MetadataAuthor
import io.thoth.metadata.responses.MetadataBook
import io.thoth.metadata.responses.MetadataSearchBook
import io.thoth.metadata.responses.MetadataSeries
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.openapi.ktor.get
import io.thoth.server.repositories.LibraryRepository
import org.koin.ktor.ext.inject

fun Routing.metadataRouting() {
    val metadataProviders by inject<MetadataProviders>()
    val libraryRepository by inject<LibraryRepository>()

    get<Api.Libraries.Id.Metadata.Search, List<MetadataSearchBook>> {
        val library = libraryRepository.raw(it.libraryId)
        val metadataProvider = MetadataWrapper.fromAgents(library.metadataScanners, metadataProviders)

        metadataProvider.search(
            region = library.language,
            keywords = it.keywords,
            title = it.title,
            author = it.author,
            narrator = it.narrator,
            language = it.language,
            pageSize = it.pageSize,
        )
    }

    get<Api.Libraries.Id.Metadata.Author.Id, MetadataAuthor> {
        val library = libraryRepository.raw(it.libraryId)
        val metadataProvider = MetadataWrapper.fromAgents(library.metadataScanners, metadataProviders)
        metadataProvider.getAuthorByID(providerId = it.provider, authorId = it.id, region = it.region)
            ?: throw ErrorResponse.notFound("Author", it.id, "Provider ${it.provider}")
    }

    get<Api.Libraries.Id.Metadata.Author.Search, List<MetadataAuthor>> {
        val library = libraryRepository.raw(it.libraryId)
        val metadataProvider = MetadataWrapper.fromAgents(library.metadataScanners, metadataProviders)
        metadataProvider.getAuthorByName(authorName = it.q, region = it.region)
    }

    get<Api.Libraries.Id.Metadata.Book.Id, MetadataBook> {
        val library = libraryRepository.raw(it.libraryId)
        val metadataProvider = MetadataWrapper.fromAgents(library.metadataScanners, metadataProviders)
        metadataProvider.getBookByID(providerId = it.provider, region = it.region, bookId = it.id)
            ?: throw ErrorResponse.notFound("Book", it.id, "Provider ${it.provider}")
    }

    get<Api.Libraries.Id.Metadata.Book.Search, List<MetadataBook>> {
        val library = libraryRepository.raw(it.libraryId)
        val metadataProvider = MetadataWrapper.fromAgents(library.metadataScanners, metadataProviders)
        metadataProvider.getBookByName(bookName = it.q, region = it.region, authorName = it.authorName)
    }

    get<Api.Libraries.Id.Metadata.Series.Id, MetadataSeries> {
        val library = libraryRepository.raw(it.libraryId)
        val metadataProvider = MetadataWrapper.fromAgents(library.metadataScanners, metadataProviders)
        metadataProvider.getSeriesByID(providerId = it.provider, region = it.region, seriesId = it.id)
            ?: throw ErrorResponse.notFound("Series", it.id, "Provider ${it.provider}")
    }
    get<Api.Libraries.Id.Metadata.Series.Search, List<MetadataSeries>> {
        val library = libraryRepository.raw(it.libraryId)
        val metadataProvider = MetadataWrapper.fromAgents(library.metadataScanners, metadataProviders)
        metadataProvider.getSeriesByName(seriesName = it.q, region = it.region, authorName = it.authorName)
    }
}
