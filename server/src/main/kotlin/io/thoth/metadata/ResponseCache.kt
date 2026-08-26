package io.thoth.metadata

import com.github.benmanes.caffeine.cache.AsyncCache
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import java.time.Duration
import kotlin.coroutines.cancellation.CancellationException

private const val MAX_CACHED_BYTES = 32L * 1024 * 1024
private val ENTRY_LIFETIME = Duration.ofHours(1)

internal class ResponseCache {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val cache: AsyncCache<String, CachedBody> =
        Caffeine
            .newBuilder()
            .maximumWeight(MAX_CACHED_BYTES)
            .weigher<String, CachedBody> { _, cached -> cached.body?.length ?: 0 }
            .expireAfterWrite(ENTRY_LIFETIME)
            .buildAsync()

    suspend fun getOrLoad(
        key: String,
        load: suspend () -> String?,
    ): String? {
        val shared = cache.get(key) { _, _ -> scope.async { CachedBody(load()) }.asCompletableFuture() }
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
                cache.asMap().remove(key, shared)
                throw e
            }
        if (result.body == null) cache.asMap().remove(key, shared)
        return result.body
    }

    private class CachedBody(
        val body: String?,
    )
}
