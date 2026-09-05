package io.thoth.metadata

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ResponseCacheTest {
    private val cache = ResponseCache()
    private val loads = CopyOnWriteArrayList<String>()

    private suspend fun load(
        key: String,
        body: suspend (String) -> String? = { it },
    ): String? =
        cache.getOrLoad(key) {
            loads += key
            body(key)
        }

    @Test
    fun `a repeated lookup is answered from the cache`() =
        runBlocking {
            load("book-1")
            load("book-1")

            assertEquals(listOf("book-1"), loads)
        }

    @Test
    fun `answers of different keys do not share an entry`() =
        runBlocking {
            load("book-1@US")
            load("book-1@DE")

            assertEquals(listOf("book-1@US", "book-1@DE"), loads)
        }

    @Test
    fun `concurrent callers share a single request`() =
        runBlocking {
            val released = CompletableDeferred<Unit>()

            val callers = (1..5).map {
                async {
                    load("book-1") {
                        released.await()
                        it
                    }
                }
            }
            while (loads.isEmpty()) delay(1)
            released.complete(Unit)

            assertEquals(List(5) { "book-1" }, callers.map { it.await() })
            assertEquals(1, loads.size)
        }

    @Test
    fun `one caller giving up does not cancel the request of the others`() =
        runBlocking {
            val released = CompletableDeferred<Unit>()

            val stays = async {
                load("book-1") {
                    released.await()
                    it
                }
            }
            val givesUp = launch {
                load("book-1") {
                    released.await()
                    it
                }
            }
            // Both callers have to arrive at the shared entry before one of them walks away
            while (loads.isEmpty()) delay(1)
            givesUp.cancelAndJoin()
            released.complete(Unit)

            assertEquals("book-1", stays.await())
            assertEquals(1, loads.size)
        }

    @Test
    fun `a failed lookup keeps its exception and is not cached`() =
        runBlocking {
            val attempts = AtomicInteger()
            val failing: suspend (String) -> String? = {
                if (attempts.incrementAndGet() == 1) throw IOException("provider is down") else it
            }

            val failure = assertFailsWith<IOException> { load("book-1", failing) }

            assertEquals("provider is down", failure.message)
            assertEquals("book-1", load("book-1", failing))
        }

    @Test
    fun `a lookup which finds nothing is not cached`() =
        runBlocking {
            assertNull(load("book-1") { null })
            assertNull(load("book-1") { null })

            assertEquals(listOf("book-1", "book-1"), loads)
        }
}
