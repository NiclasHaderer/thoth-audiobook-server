package io.thoth.metadata

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CachingMetadataProviderTest {
    @Test
    fun `a repeated lookup is answered from the cache`() =
        runBlocking {
            val provider = FakeMetadataProvider()
            val caching = CachingMetadataProvider(provider)

            caching.getBookByID("fake", "book-1", "US")
            caching.getBookByID("fake", "book-1", "US")

            assertEquals(1, provider.bookLookups.size)
        }

    @Test
    fun `the region is part of the cache key`() =
        runBlocking {
            val provider = FakeMetadataProvider()
            val caching = CachingMetadataProvider(provider)

            caching.getBookByID("fake", "book-1", "US")
            caching.getBookByID("fake", "book-1", "DE")

            assertEquals(listOf("book-1@US", "book-1@DE"), provider.bookLookups)
        }

    @Test
    fun `concurrent callers share a single request`() =
        runBlocking {
            val released = CompletableDeferred<Unit>()
            val provider = FakeMetadataProvider(resolveBook = { released.await(); testBook(it) })
            val caching = CachingMetadataProvider(provider)

            val callers = (1..5).map { async { caching.getBookByID("fake", "book-1", "US") } }
            while (provider.bookLookups.isEmpty()) delay(1)
            released.complete(Unit)

            assertEquals(5, callers.map { assertNotNull(it.await()).id.itemID }.size)
            assertEquals(1, provider.bookLookups.size)
        }

    @Test
    fun `one caller giving up does not cancel the request of the others`() =
        runBlocking {
            val released = CompletableDeferred<Unit>()
            val provider = FakeMetadataProvider(resolveBook = { released.await(); testBook(it) })
            val caching = CachingMetadataProvider(provider)

            val stays = async { caching.getBookByID("fake", "book-1", "US") }
            val givesUp = launch { caching.getBookByID("fake", "book-1", "US") }
            // Both callers have to arrive at the shared entry before one of them walks away
            while (provider.bookLookups.isEmpty()) delay(1)
            givesUp.cancelAndJoin()
            released.complete(Unit)

            assertEquals("book-1", assertNotNull(stays.await()).id.itemID)
            assertEquals(1, provider.bookLookups.size)
        }

    @Test
    fun `a failed lookup keeps its exception and is not cached`() =
        runBlocking {
            val attempts = AtomicInteger()
            val provider =
                FakeMetadataProvider(
                    resolveBook = {
                        if (attempts.incrementAndGet() == 1) throw IOException("provider is down") else testBook(it)
                    },
                )
            val caching = CachingMetadataProvider(provider)

            val failure = assertFailsWith<IOException> { caching.getBookByID("fake", "book-1", "US") }

            assertEquals("provider is down", failure.message)
            assertEquals("book-1", assertNotNull(caching.getBookByID("fake", "book-1", "US")).id.itemID)
        }

    @Test
    fun `a lookup which finds nothing is not cached`() =
        runBlocking {
            val provider = FakeMetadataProvider(resolveBook = { null })
            val caching = CachingMetadataProvider(provider)

            assertNull(caching.getBookByID("fake", "book-1", "US"))
            assertNull(caching.getBookByID("fake", "book-1", "US"))

            assertEquals(2, provider.bookLookups.size)
        }
}
