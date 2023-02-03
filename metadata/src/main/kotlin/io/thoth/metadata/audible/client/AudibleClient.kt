package io.thoth.metadata.audible.client

import io.thoth.metadata.MetadataProvider
import io.thoth.metadata.audible.models.AudibleRegions
import io.thoth.metadata.audible.models.AudibleSearchAmount
import io.thoth.metadata.audible.models.AudibleSearchLanguage
import io.thoth.metadata.responses.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import me.xdrop.fuzzywuzzy.FuzzySearch

const val AUDIBLE_PROVIDER_NAME = "audible"

class AudibleClient(
    val region: AudibleRegions,
    val imageSize: Int = 500
) : MetadataProvider {
    override val uniqueName = AUDIBLE_PROVIDER_NAME

    override suspend fun search(
        keywords: String?,
        title: String?,
        author: String?,
        narrator: String?,
        language: MetadataLanguage?,
        pageSize: MetadataSearchCount?,
    ): List<MetadataSearchBookImpl> {
        return getAudibleSearchResult(
            region,
            keywords = keywords,
            title = title,
            author = author,
            narrator = narrator,
            language = if (language != null) AudibleSearchLanguage.from(language) else null,
            pageSize = if (pageSize != null) AudibleSearchAmount.from(pageSize) else null
        )?.filter { it.title != null && it.id.itemID != "search" } ?: listOf()
    }

    override suspend fun getAuthorByID(providerId: String, authorId: String): MetadataAuthorImpl? {
        return getAudibleAuthor(region, imageSize, authorId)
    }

    override suspend fun getAuthorByName(authorName: String): List<MetadataAuthorImpl> {
        val searchResult = search(author = authorName)

        return coroutineScope {
            FuzzySearch
                .extractSorted(authorName, searchResult) { it.author!!.name }
                .map {
                    async {
                        getAuthorByID(
                            uniqueName, it.referent.author!!.id.itemID
                        )
                    }
                }
                .awaitAll()
                .distinctBy { it?.id?.itemID }
                .filterNotNull()
        }
    }

    override suspend fun getBookByID(providerId: String, bookId: String): MetadataBookImpl? {
        return getAudibleBook(region, bookId)
    }

    override suspend fun getBookByName(bookName: String, authorName: String?): List<MetadataBookImpl> {
        val searchResult = search(title = bookName, author = authorName)

        return coroutineScope {
            FuzzySearch.extractSorted(bookName, searchResult) { it.title }
                .map {
                    async {
                        getBookByID(uniqueName, it.referent.id.itemID)
                    }
                }
                .awaitAll()
                .distinctBy { it?.id?.itemID }
                .filterNotNull()
        }
    }

    override suspend fun getSeriesByID(providerId: String, seriesId: String): MetadataSeriesImpl? {
        return getAudibleSeries(region, seriesId)
    }

    override suspend fun getSeriesByName(seriesName: String, authorName: String?): List<MetadataSeriesImpl> {
        val seriesResult = search(keywords = seriesName, author = authorName).filter { it.series?.name != null }

        return coroutineScope {
            FuzzySearch.extractSorted(seriesName, seriesResult) { it.series!!.name }
                .map {
                    async {
                        getSeriesByID(uniqueName, it.referent.series!!.id.itemID)
                    }
                }
                .awaitAll()
                .distinctBy { it?.id?.itemID }
                .filterNotNull()
        }
    }
}
