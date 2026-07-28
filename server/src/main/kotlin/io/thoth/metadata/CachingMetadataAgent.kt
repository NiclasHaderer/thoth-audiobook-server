package io.thoth.metadata

import com.github.benmanes.caffeine.cache.AsyncCache
import com.github.benmanes.caffeine.cache.Caffeine
import io.thoth.metadata.responses.MetadataAuthor
import io.thoth.metadata.responses.MetadataBook
import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.metadata.responses.MetadataSearchBook
import io.thoth.metadata.responses.MetadataSearchCount
import io.thoth.metadata.responses.MetadataSeries
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import java.time.Duration

private const val KEY_SEPARATOR = "--thoth--"
private const val MAX_ENTRIES_PER_OPERATION = 50L
private val ENTRY_LIFETIME = Duration.ofHours(1)

/**
 * Caches the answers of another agent. The entries hold the in-flight request instead of its result, so concurrent
 * callers asking for the same thing share one request to the provider. Requests which end in an exception or find
 * nothing are dropped from the cache instead of being served for the rest of the hour.
 */
class CachingMetadataAgent(
    private val delegate: MetadataAgent,
) : MetadataAgent {
    override val name get() = delegate.name
    override val supportedCountryCodes get() = delegate.supportedCountryCodes

    // Entries outlive the request which created them, so they must not be tied to its coroutine scope
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val searchCache = buildCache<List<MetadataSearchBook>>()
    private val authorNameCache = buildCache<List<MetadataAuthor>>()
    private val bookNameCache = buildCache<List<MetadataBook>>()
    private val seriesNameCache = buildCache<List<MetadataSeries>>()
    private val authorIdCache = buildCache<MetadataAuthor?>()
    private val bookIdCache = buildCache<MetadataBook?>()
    private val seriesIdCache = buildCache<MetadataSeries?>()

    override suspend fun search(
        region: String,
        keywords: String?,
        title: String?,
        author: String?,
        narrator: String?,
        language: MetadataLanguage?,
        pageSize: MetadataSearchCount?,
    ): List<MetadataSearchBook> =
        searchCache.getOrLoad(keyOf(region, keywords, title, author, narrator, language, pageSize)) {
            delegate.search(region, keywords, title, author, narrator, language, pageSize)
        }

    override suspend fun getAuthorByID(
        providerId: String,
        authorId: String,
        region: String,
    ): MetadataAuthor? =
        authorIdCache.getOrLoad(keyOf(providerId, authorId, region)) {
            delegate.getAuthorByID(providerId, authorId, region)
        }

    override suspend fun getAuthorByName(
        authorName: String,
        region: String,
    ): List<MetadataAuthor> =
        authorNameCache.getOrLoad(keyOf(authorName, region)) {
            delegate.getAuthorByName(authorName, region)
        }

    override suspend fun getBookByID(
        providerId: String,
        bookId: String,
        region: String,
    ): MetadataBook? =
        bookIdCache.getOrLoad(keyOf(providerId, bookId, region)) {
            delegate.getBookByID(providerId, bookId, region)
        }

    override suspend fun getBookByName(
        bookName: String,
        region: String,
        authorName: String?,
    ): List<MetadataBook> =
        bookNameCache.getOrLoad(keyOf(bookName, region, authorName)) {
            delegate.getBookByName(bookName, region, authorName)
        }

    override suspend fun getSeriesByID(
        providerId: String,
        seriesId: String,
        region: String,
    ): MetadataSeries? =
        seriesIdCache.getOrLoad(keyOf(providerId, seriesId, region)) {
            delegate.getSeriesByID(providerId, seriesId, region)
        }

    override suspend fun getSeriesByName(
        seriesName: String,
        region: String,
        authorName: String?,
    ): List<MetadataSeries> =
        seriesNameCache.getOrLoad(keyOf(seriesName, region, authorName)) {
            delegate.getSeriesByName(seriesName, region, authorName)
        }

    private fun <T> buildCache(): AsyncCache<String, T> =
        Caffeine
            .newBuilder()
            .maximumSize(MAX_ENTRIES_PER_OPERATION)
            .expireAfterWrite(ENTRY_LIFETIME)
            .buildAsync()

    private fun keyOf(vararg parts: Any?): String = parts.joinToString(KEY_SEPARATOR)

    private suspend fun <T> AsyncCache<String, T>.getOrLoad(
        key: String,
        load: suspend () -> T,
    ): T = get(key) { _, _ -> scope.async { load() }.asCompletableFuture() }.await()
}
