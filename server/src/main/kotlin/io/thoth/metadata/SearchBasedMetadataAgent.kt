package io.thoth.metadata

import io.thoth.metadata.responses.MetadataAuthor
import io.thoth.metadata.responses.MetadataBook
import io.thoth.metadata.responses.MetadataSeries
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import me.xdrop.fuzzywuzzy.FuzzySearch

/**
 * Agent for providers which only offer a book search, so looking an entity up by name means searching for it, ranking
 * the hits by name similarity and resolving the referenced IDs.
 */
abstract class SearchBasedMetadataAgent : MetadataAgent {
    override suspend fun getAuthorByName(
        authorName: String,
        region: String,
    ): List<MetadataAuthor> {
        val hits = search(region = region, author = authorName)
        val authorIds =
            FuzzySearch
                .extractSorted(authorName, hits) { hit -> hit.authors?.joinToString(", ") { it.name ?: "" } ?: "" }
                .flatMap { it.referent.authors ?: emptyList() }
                .map { it.id.itemID }
                .distinct()

        return resolveAll(authorIds) { getAuthorByID(providerId = name, authorId = it, region = region) }
    }

    override suspend fun getBookByName(
        bookName: String,
        region: String,
        authorName: String?,
    ): List<MetadataBook> {
        val hits = search(region = region, title = bookName, author = authorName)
        val bookIds =
            FuzzySearch
                .extractSorted(bookName, hits) { it.title ?: "" }
                .map { it.referent.id.itemID }
                .distinct()

        return resolveAll(bookIds) { getBookByID(providerId = name, bookId = it, region = region) }
    }

    override suspend fun getSeriesByName(
        seriesName: String,
        region: String,
        authorName: String?,
    ): List<MetadataSeries> {
        val hits = search(region = region, keywords = seriesName, author = authorName).flatMap { it.series }
        val seriesIds =
            FuzzySearch
                .extractSorted(seriesName, hits) { it.title ?: "" }
                .map { it.referent.id.itemID }
                .distinct()

        return resolveAll(seriesIds) { getSeriesByID(providerId = name, seriesId = it, region = region) }
    }

    private suspend fun <T> resolveAll(
        ids: List<String>,
        resolve: suspend (String) -> T?,
    ): List<T> =
        coroutineScope {
            ids.map { async { resolve(it) } }.awaitAll().filterNotNull()
        }
}
