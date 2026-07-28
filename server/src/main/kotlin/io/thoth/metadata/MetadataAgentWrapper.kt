package io.thoth.metadata

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.thoth.metadata.responses.MetadataAuthor
import io.thoth.metadata.responses.MetadataBook
import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.metadata.responses.MetadataSearchBook
import io.thoth.metadata.responses.MetadataSearchCount
import io.thoth.metadata.responses.MetadataSeries
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import me.xdrop.fuzzywuzzy.FuzzySearch

private val log = logger {}

class MetadataAgentWrapper(
    private val providerList: List<MetadataAgent>,
) : MetadataAgent {
    override val name = providerList.joinToString(", ") { it.name }
    override val supportedCountryCodes: List<String>
        get() = providerList.flatMap { it.supportedCountryCodes }.distinct()

    private val providerMap by lazy { providerList.associateBy { it.name } }

    override suspend fun search(
        region: String,
        keywords: String?,
        title: String?,
        author: String?,
        narrator: String?,
        language: MetadataLanguage?,
        pageSize: MetadataSearchCount?,
    ): List<MetadataSearchBook> =
        fromAllProviders {
            it.search(
                region = region,
                keywords = keywords,
                title = title,
                author = author,
                narrator = narrator,
                language = language,
                pageSize = pageSize,
            )
        }

    override suspend fun getAuthorByID(
        providerId: String,
        authorId: String,
        region: String,
    ): MetadataAuthor? =
        provider(providerId)?.getAuthorByID(providerId = providerId, authorId = authorId, region = region)

    override suspend fun getBookByID(
        providerId: String,
        bookId: String,
        region: String,
    ): MetadataBook? = provider(providerId)?.getBookByID(providerId = providerId, bookId = bookId, region = region)

    override suspend fun getSeriesByID(
        providerId: String,
        seriesId: String,
        region: String,
    ): MetadataSeries? =
        provider(providerId)?.getSeriesByID(providerId = providerId, seriesId = seriesId, region = region)

    override suspend fun getAuthorByName(
        authorName: String,
        region: String,
    ): List<MetadataAuthor> {
        val authors = fromAllProviders { it.getAuthorByName(authorName = authorName, region = region) }
        return rankByName(authorName, authors) { it.name }
    }

    override suspend fun getBookByName(
        bookName: String,
        region: String,
        authorName: String?,
    ): List<MetadataBook> {
        val books = fromAllProviders { it.getBookByName(bookName = bookName, region = region, authorName = authorName) }
        return rankByName(bookName, books) { it.title }
    }

    override suspend fun getSeriesByName(
        seriesName: String,
        region: String,
        authorName: String?,
    ): List<MetadataSeries> {
        val series =
            fromAllProviders { it.getSeriesByName(seriesName = seriesName, region = region, authorName = authorName) }
        return rankByName(seriesName, series) { it.title }
    }

    private suspend fun <T> fromAllProviders(query: suspend (MetadataAgent) -> List<T>): List<T> =
        coroutineScope {
            providerList.map { async { query(it) } }.awaitAll().flatten()
        }

    private fun <T> rankByName(
        name: String,
        items: List<T>,
        nameOf: (T) -> String?,
    ): List<T> =
        FuzzySearch
            .extractSorted(name, items.filter { nameOf(it) != null }) { nameOf(it) }
            .map { it.referent }

    private fun provider(providerId: String): MetadataAgent? {
        val provider = providerMap[providerId]
        if (provider == null) {
            log.warn { "No metadata agent named '$providerId' (available: ${providerMap.keys})" }
        }
        return provider
    }
}
