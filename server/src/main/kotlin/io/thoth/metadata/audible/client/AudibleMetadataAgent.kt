package io.thoth.metadata.audible.client

import io.thoth.metadata.MetadataAgent
import io.thoth.metadata.audible.models.AudibleRegions
import io.thoth.metadata.audible.models.AudibleSearchAmount
import io.thoth.metadata.audible.models.AudibleSearchLanguage
import io.thoth.metadata.responses.MetadataAuthorImpl
import io.thoth.metadata.responses.MetadataBookImpl
import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.metadata.responses.MetadataSearchBookImpl
import io.thoth.metadata.responses.MetadataSearchCount
import io.thoth.metadata.responses.MetadataSeriesImpl
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import me.xdrop.fuzzywuzzy.FuzzySearch

const val AUDIBLE_PROVIDER_NAME = "audible"

class AudibleMetadataAgent(
    private val imageSize: Int = 500,
) : MetadataAgent() {
    override val name = AUDIBLE_PROVIDER_NAME

    override val supportedCountryCodes: List<String>
        get() = AudibleRegions.entries.map { it.name }

    override suspend fun searchImpl(
        region: String,
        keywords: String?,
        title: String?,
        author: String?,
        narrator: String?,
        language: MetadataLanguage?,
        pageSize: MetadataSearchCount?,
    ): List<MetadataSearchBookImpl> {
        val audibleRegion = AudibleRegions.from(region)
        return getAudibleSearchResult(
            audibleRegion,
            keywords = keywords,
            title = title,
            author = author,
            narrator = narrator,
            language = if (language != null) AudibleSearchLanguage.from(language) else null,
            pageSize = if (pageSize != null) AudibleSearchAmount.from(pageSize) else null,
        )?.filter { it.title != null && it.id.itemID != "search" } ?: listOf()
    }

    override suspend fun getAuthorByIDImpl(
        providerId: String,
        authorId: String,
        region: String,
    ): MetadataAuthorImpl? {
        val audibleRegion = AudibleRegions.from(region)

        return getAudibleAuthor(audibleRegion, imageSize, authorId)
    }

    override suspend fun getAuthorByNameImpl(
        authorName: String,
        region: String,
    ): List<MetadataAuthorImpl> {
        val searchResult = searchImpl(region, author = authorName)

        return coroutineScope {
            FuzzySearch
                .extractSorted(authorName, searchResult) { search ->
                    search.authors!!.joinToString(", ") { it.name ?: "" }
                }.map {
                    async {
                        it.referent.authors?.map { author ->
                            getAuthorByIDImpl(providerId = name, authorId = author.id.itemID, region = region)
                        }
                    }
                }.awaitAll()
                .filterNotNull()
                .flatten()
                .distinctBy { it?.id?.itemID }
                .filterNotNull()
        }
    }

    override suspend fun getBookByIDImpl(
        providerId: String,
        bookId: String,
        region: String,
    ): MetadataBookImpl? {
        val audibleRegion = AudibleRegions.from(region)
        return getAudibleBook(audibleRegion, bookId)
    }

    override suspend fun getBookByNameImpl(
        bookName: String,
        region: String,
        authorName: String?,
    ): List<MetadataBookImpl> {
        val searchResult = searchImpl(region, title = bookName, author = authorName)

        return coroutineScope {
            FuzzySearch
                .extractSorted(bookName, searchResult) { it.title }
                .map {
                    async { getBookByIDImpl(providerId = name, region = region, bookId = it.referent.id.itemID) }
                }.awaitAll()
                .distinctBy { it?.id?.itemID }
                .filterNotNull()
        }
    }

    override suspend fun getSeriesByIDImpl(
        providerId: String,
        region: String,
        seriesId: String,
    ): MetadataSeriesImpl? {
        val audibleRegion = AudibleRegions.from(region)
        return getAudibleSeries(audibleRegion, seriesId)
    }

    override suspend fun getSeriesByNameImpl(
        seriesName: String,
        region: String,
        authorName: String?,
    ): List<MetadataSeriesImpl> {
        val seriesResult =
            searchImpl(region = region, keywords = seriesName, author = authorName)
                .flatMap { it.series }
                .filter { it.title != null }

        return coroutineScope {
            FuzzySearch
                .extractSorted(seriesName, seriesResult) { it.title }
                .map {
                    async {
                        getSeriesByIDImpl(
                            providerId = name,
                            region = region,
                            seriesId = it.referent.id.itemID,
                        )
                    }
                }.awaitAll()
                .distinctBy { it?.id?.itemID }
                .filterNotNull()
        }
    }
}
