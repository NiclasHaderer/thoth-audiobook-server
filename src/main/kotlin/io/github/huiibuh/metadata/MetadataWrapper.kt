package io.github.huiibuh.metadata

import me.xdrop.fuzzywuzzy.FuzzySearch

class MetadataWrapper constructor(
    private val providerList: List<MetadataProvider>,
) : MetadataProvider {
    override var uniqueName = "MetadataWrapper"

    private val providerMap by lazy { providerList.associateBy { it.uniqueName } }

    override suspend fun search(
        keywords: String?,
        title: String?,
        author: String?,
        narrator: String?,
        language: MetadataLanguage?,
        pageSize: MetadataSearchCount?,
    ): List<SearchResultMetadata> {
        return providerList.flatMap { it.search(keywords, title, author, narrator, language, pageSize) }
    }

    override suspend fun getAuthorByID(authorID: ProviderWithID): AuthorMetadata? {
        val provider = providerMap[authorID.uniqueProviderName] ?: return null
        return provider.getAuthorByID(authorID)
    }

    override suspend fun getAuthorByName(authorName: String): AuthorMetadata? {
        val authors = providerList.map { it.getAuthorByName(authorName) }.filter { it?.name != null }
        if (authors.isEmpty()) return null

        return FuzzySearch.extractOne(authorName, authors) { it?.name }.referent
    }

    override suspend fun getBookByID(bookID: ProviderWithID): BookMetadata? {
        val provider = providerMap[bookID.uniqueProviderName] ?: return null
        return provider.getBookByID(bookID)
    }

    override suspend fun getBookByName(bookName: String): BookMetadata? {
        val books = providerList.map { it.getBookByName(bookName) }.filter { it?.title != null }
        if (books.isEmpty()) return null

        return FuzzySearch.extractOne(bookName, books) { it?.title }.referent
    }

    override suspend fun getSeriesByID(seriesID: ProviderWithID): SeriesMetadata? {
        val provider = providerMap[seriesID.uniqueProviderName] ?: return null
        return provider.getSeriesByID(seriesID)
    }

    override suspend fun getSeriesByName(seriesName: String): SeriesMetadata? {
        val series = providerList.map { it.getSeriesByName(seriesName) }.filter { it?.name != null }
        if (series.isEmpty()) return null

        return FuzzySearch.extractOne(seriesName, series) { it?.name }.referent
    }
}
