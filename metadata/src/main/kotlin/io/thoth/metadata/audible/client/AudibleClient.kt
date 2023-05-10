package io.thoth.metadata.audible.client

import io.thoth.metadata.MetadataProvider
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

class AudibleClient(private val imageSize: Int = 500) : MetadataProvider() {
    override val uniqueName = AUDIBLE_PROVIDER_NAME

    override val supportedCountryCodes: List<String>
        get() = AudibleRegions.values().map { it.name }

    override suspend fun _search(
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
            )
            ?.filter { it.title != null && it.id.itemID != "search" }
            ?: listOf()
    }

    override suspend fun _getAuthorByID(providerId: String, authorId: String, region: String): MetadataAuthorImpl? {
        val audibleRegion = AudibleRegions.from(region)

        return getAudibleAuthor(audibleRegion, imageSize, authorId)
    }

    override suspend fun _getAuthorByName(authorName: String, region: String): List<MetadataAuthorImpl> {

        val searchResult = _search(region, author = authorName)

        return coroutineScope {
            FuzzySearch.extractSorted(authorName, searchResult) { it.author!!.name }
                .map {
                    async {
                        _getAuthorByID(
                            providerId = uniqueName,
                            authorId = it.referent.author!!.id.itemID,
                            region = region,
                        )
                    }
                }
                .awaitAll()
                .distinctBy { it?.id?.itemID }
                .filterNotNull()
        }
    }

    override suspend fun _getBookByID(providerId: String, bookId: String, region: String): MetadataBookImpl? {
        val audibleRegion = AudibleRegions.from(region)
        return getAudibleBook(audibleRegion, bookId)
    }

    override suspend fun _getBookByName(bookName: String, region: String, authorName: String?): List<MetadataBookImpl> {
        val searchResult = _search(region, title = bookName, author = authorName)

        return coroutineScope {
            FuzzySearch.extractSorted(bookName, searchResult) { it.title }
                .map {
                    async {
                        _getBookByID(
                            providerId = uniqueName,
                            region = region,
                            bookId = it.referent.id.itemID,
                        )
                    }
                }
                .awaitAll()
                .distinctBy { it?.id?.itemID }
                .filterNotNull()
        }
    }

    override suspend fun _getSeriesByID(providerId: String, region: String, seriesId: String): MetadataSeriesImpl? {
        val audibleRegion = AudibleRegions.from(region)
        return getAudibleSeries(audibleRegion, seriesId)
    }

    override suspend fun _getSeriesByName(
        seriesName: String,
        region: String,
        authorName: String?
    ): List<MetadataSeriesImpl> {
        val seriesResult =
            _search(region = region, keywords = seriesName, author = authorName)
                .flatMap { it.series }
                .filter { it.title != null }

        return coroutineScope {
            FuzzySearch.extractSorted(seriesName, seriesResult) { it.title }
                .map {
                    async {
                        _getSeriesByID(
                            providerId = uniqueName,
                            region = region,
                            seriesId = it.referent.id.itemID,
                        )
                    }
                }
                .awaitAll()
                .distinctBy { it?.id?.itemID }
                .filterNotNull()
        }
    }
}
