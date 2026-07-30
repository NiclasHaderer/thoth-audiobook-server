package io.thoth.metadata

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A lookup by name resolves one provider ID per result, so what matters is how many of them a caller has to pay for.
 */
class SearchBasedMetadataAgentTest {
    private fun agentFor(vararg hits: String) = FakeMetadataProvider(hits = hits.map { searchHit(it) })

    @Test
    fun `nothing is requested before the flow is collected`() =
        runBlocking {
            val provider = agentFor("a", "b")

            SearchBasedMetadataAgent(provider).getBookByName("a", "US")

            assertEquals(0, provider.searchCalls.get())
            assertEquals(emptyList(), provider.bookLookups)
        }

    @Test
    fun `taking the best match resolves one window instead of every hit`() =
        runBlocking {
            val provider = agentFor(*Array(20) { "book-$it" })

            val best = SearchBasedMetadataAgent(provider).getBookByName("book-0", "US").first()

            assertEquals("book-0", best.id.itemID)
            assertEquals(1, provider.searchCalls.get())
            assertEquals(5, provider.bookLookups.size)
        }

    @Test
    fun `the second window is only resolved once the first one is used up`() =
        runBlocking {
            val provider = agentFor(*Array(20) { "book-$it" })

            SearchBasedMetadataAgent(provider).getBookByName("book-0", "US").take(6).toList()

            assertEquals(10, provider.bookLookups.size)
        }

    @Test
    fun `collecting everything resolves every hit once, best match first`() =
        runBlocking {
            val provider = agentFor("Moby Dick", "Dick", "Moby Dick and Friends")

            val books = SearchBasedMetadataAgent(provider).getBookByName("Moby Dick", "US").toList()

            assertEquals("Moby Dick", books.first().id.itemID)
            assertEquals(3, books.size)
            assertEquals(3, provider.bookLookups.size)
        }

    @Test
    fun `hits which cannot be resolved are skipped instead of ending the flow`() =
        runBlocking {
            val provider =
                FakeMetadataProvider(
                    hits = listOf("a", "b", "c").map { searchHit(it) },
                    resolveBook = { if (it == "a") null else testBook(it) },
                )

            val books = SearchBasedMetadataAgent(provider).getBookByName("a", "US").toList()

            assertEquals(listOf("b", "c"), books.map { it.id.itemID })
        }

    @Test
    fun `an author shared by several hits is only looked up once`() =
        runBlocking {
            val provider =
                FakeMetadataProvider(
                    hits =
                        listOf(
                            searchHit("book-1", authors = listOf("Twain")),
                            searchHit("book-2", authors = listOf("Twain")),
                        ),
                )

            val authors = SearchBasedMetadataAgent(provider).getAuthorByName("Twain", "US").toList()

            assertEquals(listOf("Twain"), authors.map { it.id.itemID })
            assertEquals(listOf("Twain@US"), provider.authorLookups)
        }

    @Test
    fun `the series of the hits are resolved, not the hits themselves`() =
        runBlocking {
            val provider =
                FakeMetadataProvider(hits = listOf(searchHit("book-1", series = listOf("Discworld"))))

            val series = SearchBasedMetadataAgent(provider).getSeriesByName("Discworld", "US").toList()

            assertEquals(listOf("Discworld"), series.map { it.id.itemID })
            assertEquals(listOf("Discworld@US"), provider.seriesLookups)
            assertEquals(emptyList(), provider.bookLookups)
        }
}
