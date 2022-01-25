package io.github.huiibuh.metadata

import io.github.huiibuh.api.exceptions.APIBadRequest
import io.ktor.features.*
import me.xdrop.fuzzywuzzy.FuzzySearch

class MetadataWrapper constructor(
    private val providerList: List<MetadataProvider>,
) : MetadataProviderWrapper {
    override var uniqueName = "MetadataWrapper"

    private val providerMap by lazy { providerList.associateBy { it.uniqueName } }

    override suspend fun search(
        keywords: String?,
        title: String?,
        author: String?,
        narrator: String?,
        language: MetadataLanguage?,
        pageSize: MetadataSearchCount?,
    ): List<SearchBookMetadata> {
        return providerList.flatMap { it.search(keywords, title, author, narrator, language, pageSize) }
    }

    @Throws(NotFoundException::class, APIBadRequest::class)
    override suspend fun getAuthorByID(authorID: ProviderWithIDMetadata): AuthorMetadata {
        val provider = getProvider(authorID)
        return provider.getAuthorByID(authorID)
            ?: throw NotFoundException("Could not find author with id ${authorID.itemID}")
    }

    override suspend fun getAuthorByName(authorName: String): AuthorMetadata? {
        val authors = providerList.map { it.getAuthorByName(authorName) }.filter { it?.name != null }
        if (authors.isEmpty()) return null

        return FuzzySearch.extractOne(authorName, authors) { it?.name }.referent
    }

    @Throws(NotFoundException::class, APIBadRequest::class)
    override suspend fun getBookByID(bookID: ProviderWithIDMetadata): BookMetadata {
        val provider = getProvider(bookID)
        return provider.getBookByID(bookID)
            ?: throw NotFoundException("Could not find book with id ${bookID.itemID}")
    }

    override suspend fun getBookByName(bookName: String, authorName: String?): BookMetadata? {
        val books = providerList.map { it.getBookByName(bookName, authorName) }.filter { it?.title != null }
        if (books.isEmpty()) return null

        return FuzzySearch.extractOne(bookName, books) { it?.title }.referent
    }

    @Throws(NotFoundException::class, APIBadRequest::class)
    override suspend fun getSeriesByID(seriesID: ProviderWithIDMetadata): SeriesMetadata {
        val provider = getProvider(seriesID)
        return provider.getSeriesByID(seriesID)
            ?: throw NotFoundException("Could not find book with id ${seriesID.itemID}")
    }

    override suspend fun getSeriesByName(seriesName: String, authorName: String?): SeriesMetadata? {
        val series = providerList.map { it.getSeriesByName(seriesName, authorName) }.filter { it?.name != null }
        if (series.isEmpty()) return null

        return FuzzySearch.extractOne(seriesName, series) { it?.name }.referent
    }

    @kotlin.jvm.Throws(APIBadRequest::class)
    private fun getProvider(providerID: ProviderWithIDMetadata) =
        providerMap[providerID.provider] ?: throw APIBadRequest("Provider with id ${providerID.provider} was not found")
}
