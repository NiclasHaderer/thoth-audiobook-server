package io.thoth.metadata

import io.thoth.metadata.responses.MetadataAuthor
import io.thoth.metadata.responses.MetadataBook
import io.thoth.metadata.responses.MetadataSeries
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import me.xdrop.fuzzywuzzy.FuzzySearch

/** Number of IDs which are resolved at once while walking the ranked hits of a search. */
private const val RESOLUTION_WINDOW = 5

/**
 * Agent for providers which only offer a book search, so looking an entity up by name means searching for it, ranking
 * the hits by name similarity and resolving the referenced IDs. Resolving is what costs requests, so it happens in
 * windows while the returned flow is collected: taking the best match of a search with 50 hits costs one search and one
 * window instead of 50 lookups.
 */
class SearchBasedMetadataAgent(
    private val provider: MetadataProvider,
) : MetadataAgent,
    MetadataProvider by provider {
    override fun getAuthorByName(
        authorName: String,
        region: String,
    ): Flow<MetadataAuthor> =
        flow {
            val hits = search(region = region, author = authorName)
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
        region: String,
        authorName: String?,
    ): Flow<MetadataBook> =
        flow {
            val hits = search(region = region, title = bookName, author = authorName)
            val bookIds =
                FuzzySearch
                    .extractSorted(bookName, hits) { it.title ?: "" }
                    .map { it.referent.id.itemID }
                    .distinct()

            emitAll(resolveInWindows(bookIds) { getBookByID(providerId = name, bookId = it, region = region) })
        }

    override fun getSeriesByName(
        seriesName: String,
        region: String,
        authorName: String?,
    ): Flow<MetadataSeries> =
        flow {
            val hits = search(region = region, keywords = seriesName, author = authorName).flatMap { it.series }
            val seriesIds =
                FuzzySearch
                    .extractSorted(seriesName, hits) { it.title ?: "" }
                    .map { it.referent.id.itemID }
                    .distinct()

            emitAll(resolveInWindows(seriesIds) { getSeriesByID(providerId = name, seriesId = it, region = region) })
        }

    private fun <T> resolveInWindows(
        ids: List<String>,
        resolve: suspend (String) -> T?,
    ): Flow<T> =
        flow {
            ids.chunked(RESOLUTION_WINDOW).forEach { window ->
                val resolved = coroutineScope { window.map { async { resolve(it) } }.awaitAll() }
                resolved.filterNotNull().forEach { emit(it) }
            }
        }
}
