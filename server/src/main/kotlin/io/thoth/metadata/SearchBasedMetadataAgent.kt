package io.thoth.metadata

import io.thoth.metadata.responses.MetadataAuthor
import io.thoth.metadata.responses.MetadataBook
import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.metadata.responses.MetadataRegion
import io.thoth.metadata.responses.MetadataSearchBook
import io.thoth.metadata.responses.MetadataSearchCount
import io.thoth.metadata.responses.MetadataSeries
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import me.xdrop.fuzzywuzzy.FuzzySearch

abstract class SearchBasedMetadataAgent : MetadataAgent {
    abstract suspend fun search(
        region: MetadataRegion,
        keywords: String? = null,
        title: String? = null,
        author: String? = null,
        narrator: String? = null,
        language: MetadataLanguage? = null,
        pageSize: MetadataSearchCount? = null,
    ): List<MetadataSearchBook>

    override fun getAuthorByName(
        authorName: String,
        region: MetadataRegion,
        language: MetadataLanguage?,
    ): Flow<MetadataAuthor> =
        flow {
            val hits = search(region = region, author = authorName, language = language)
            val authorIds =
                FuzzySearch
                    .extractSorted(authorName, hits) { hit -> hit.authors?.joinToString(", ") { it.name ?: "" } ?: "" }
                    .flatMap { it.referent.authors ?: emptyList() }
                    .map { it.id.itemID }
                    .distinct()

            emitAll(resolveInWindows(authorIds) { getAuthorByID(providerId = name, authorId = it, region = region) })
        }

    override fun getBookByName(
        bookName: String,
        region: MetadataRegion,
        keywords: String?,
        authorName: String?,
        narrator: String?,
        language: MetadataLanguage?,
    ): Flow<MetadataBook> =
        flow {
            // The narrator is deliberately not part of the query: the providers filter on it, which would drop every
            // edition when a book is tagged with a narrator they spell differently
            val hits =
                search(region = region, keywords = keywords, title = bookName, author = authorName, language = language)
            val bookIds =
                FuzzySearch
                    .extractSorted(bookName, hits) { it.title ?: "" }
                    .map { it.referent }
                    .narratorFirst(narrator)
                    .map { it.id.itemID }
                    .distinct()

            emitAll(resolveInWindows(bookIds) { getBookByID(providerId = name, bookId = it, region = region) })
        }

    override fun getSeriesByName(
        seriesName: String,
        region: MetadataRegion,
        authorName: String?,
        language: MetadataLanguage?,
    ): Flow<MetadataSeries> =
        flow {
            val hits =
                search(region = region, keywords = seriesName, author = authorName, language = language)
                    .flatMap { it.series }
            val seriesIds =
                FuzzySearch
                    .extractSorted(seriesName, hits) { it.title ?: "" }
                    .map { it.referent.id.itemID }
                    .distinct()

            emitAll(resolveInWindows(seriesIds) { getSeriesByID(providerId = name, seriesId = it, region = region) })
        }
}
