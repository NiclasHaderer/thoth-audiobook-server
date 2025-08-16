package io.thoth.metadata

import io.thoth.metadata.responses.MetadataAuthor
import io.thoth.metadata.responses.MetadataBook
import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.metadata.responses.MetadataSearchBook
import io.thoth.metadata.responses.MetadataSearchCount
import io.thoth.metadata.responses.MetadataSeries
import io.thoth.models.MetadataAgent
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import me.xdrop.fuzzywuzzy.FuzzySearch

class MetadataWrapper(
    private val providerList: List<MetadataProvider>,
) : MetadataProvider() {
    companion object {
        fun fromAgents(
            agents: List<MetadataAgent>,
            availableAgents: List<MetadataProvider>,
        ): MetadataProvider {
            val agentsToUse =
                availableAgents.filter { agentInstance -> agents.any { it.name == agentInstance.uniqueName } }
            return MetadataWrapper(agentsToUse)
        }
    }

    override var uniqueName = "All Providers"
    override val supportedCountryCodes: List<String>
        get() = providerList.flatMap { it.supportedCountryCodes }.distinct()

    private val providerMap by lazy { providerList.associateBy { it.uniqueName } }

    override suspend fun _search(
        region: String,
        keywords: String?,
        title: String?,
        author: String?,
        narrator: String?,
        language: MetadataLanguage?,
        pageSize: MetadataSearchCount?,
    ): List<MetadataSearchBook> =
        providerList
            .map {
                coroutineScope {
                    async {
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
                }
            }.awaitAll()
            .flatten()

    override suspend fun _getAuthorByID(
        providerId: String,
        authorId: String,
        region: String,
    ): MetadataAuthor? {
        val provider = getProvider(authorId) ?: return null
        return provider.getAuthorByID(providerId = providerId, region = region, authorId = authorId)
    }

    override suspend fun _getBookByID(
        providerId: String,
        bookId: String,
        region: String,
    ): MetadataBook? {
        val provider = getProvider(bookId) ?: return null
        return provider.getBookByID(providerId = providerId, region = region, bookId = bookId)
    }

    override suspend fun _getSeriesByID(
        providerId: String,
        region: String,
        seriesId: String,
    ): MetadataSeries? {
        val provider = getProvider(seriesId) ?: return null
        return provider.getSeriesByID(providerId = providerId, region = region, seriesId = seriesId)
    }

    override suspend fun _getAuthorByName(
        authorName: String,
        region: String,
    ): List<MetadataAuthor> {
        val authors =
            providerList
                .map { coroutineScope { async { it.getAuthorByName(authorName = authorName, region = region) } } }
                .awaitAll()
                .flatten()
                .filter { it.name != null }
        return FuzzySearch.extractSorted(authorName, authors) { it.name }.map { it.referent }
    }

    override suspend fun _getBookByName(
        bookName: String,
        region: String,
        authorName: String?,
    ): List<MetadataBook> {
        val books =
            providerList
                .map {
                    coroutineScope {
                        async { it.getBookByName(bookName = bookName, region = region, authorName = authorName) }
                    }
                }.awaitAll()
                .flatten()
                .filter { it.title != null }
        return FuzzySearch.extractSorted(bookName, books) { it.title }.map { it.referent }
    }

    override suspend fun _getSeriesByName(
        seriesName: String,
        region: String,
        authorName: String?,
    ): List<MetadataSeries> {
        val series =
            providerList
                .map {
                    coroutineScope {
                        async { it.getSeriesByName(seriesName = seriesName, region = region, authorName = authorName) }
                    }
                }.awaitAll()
                .flatten()
                .filter { it.title != null }
        return FuzzySearch.extractSorted(seriesName, series) { it.title }.map { it.referent }
    }

    private fun getProvider(providerID: String): MetadataProvider? = providerMap[providerID]
}
