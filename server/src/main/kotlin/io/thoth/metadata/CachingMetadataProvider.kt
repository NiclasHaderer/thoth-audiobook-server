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
import kotlin.coroutines.cancellation.CancellationException

private const val KEY_SEPARATOR = "--thoth--"
private const val MAX_ENTRIES_PER_OPERATION = 50L
private val ENTRY_LIFETIME = Duration.ofHours(1)

/**
 * Caches the answers of another provider. The entries hold the in-flight request instead of its result, so concurrent
 * callers asking for the same thing share one request to the provider. Requests which end in an exception, and ID
 * lookups which do not find anything, are dropped from the cache right away instead of being served for the rest of the
 * hour.
 */
class CachingMetadataProvider(
    private val delegate: MetadataProvider,
) : MetadataProvider {
    override val name get() = delegate.name
    override val supportedCountryCodes get() = delegate.supportedCountryCodes

    // Entries outlive the request which created them, so they must not be tied to its coroutine scope
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val searchCache = buildCache<List<MetadataSearchBook>>()
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

    override suspend fun getBookByID(
        providerId: String,
        bookId: String,
        region: String,
    ): MetadataBook? =
        bookIdCache.getOrLoad(keyOf(providerId, bookId, region)) {
            delegate.getBookByID(providerId, bookId, region)
        }

    override suspend fun getSeriesByID(
        providerId: String,
        seriesId: String,
        region: String,
    ): MetadataSeries? =
        seriesIdCache.getOrLoad(keyOf(providerId, seriesId, region)) {
            delegate.getSeriesByID(providerId, seriesId, region)
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
    ): T {
        val shared = get(key) { _, _ -> scope.async { load() }.asCompletableFuture() }
        // A copy is awaited because awaiting cancels the future it waits on, and the shared one belongs to every other
        // caller of this entry as well
        val result =
            try {
                shared.copy().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // Caffeine drops these entries by itself, but only once its own completion callback got around to it,
                // which can be after this caller already retried
                asMap().remove(key, shared)
                throw e
            }
        if (result == null) asMap().remove(key, shared)
        return result
    }
}
