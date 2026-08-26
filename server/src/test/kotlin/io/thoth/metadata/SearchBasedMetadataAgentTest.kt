package io.thoth.metadata

import io.thoth.metadata.responses.MetadataRegion
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
    private fun agentFor(vararg hits: String) = FakeMetadataAgent(hits = hits.map { searchHit(it) })

    @Test
    fun `nothing is requested before the flow is collected`() =
        runBlocking {
            val provider = agentFor("a", "b")

            provider.getBookByName("a", MetadataRegion.US)

            assertEquals(0, provider.searchCalls.get())
            assertEquals(emptyList(), provider.bookLookups)
        }

    @Test
    fun `taking the best match resolves one window instead of every hit`() =
        runBlocking {
            val provider = agentFor(*Array(20) { "book-$it" })

            val best = provider.getBookByName("book-0", MetadataRegion.US).first()

            assertEquals("book-0", best.id.itemID)
            assertEquals(1, provider.searchCalls.get())
            assertEquals(5, provider.bookLookups.size)
        }

    @Test
    fun `the second window is only resolved once the first one is used up`() =
        runBlocking {
            val provider = agentFor(*Array(20) { "book-$it" })

            provider.getBookByName("book-0", MetadataRegion.US).take(6).toList()

            assertEquals(10, provider.bookLookups.size)
        }

    @Test
    fun `collecting everything resolves every hit once, best match first`() =
        runBlocking {
            val provider = agentFor("Moby Dick", "Dick", "Moby Dick and Friends")

            val books = provider.getBookByName("Moby Dick", MetadataRegion.US).toList()

            assertEquals("Moby Dick", books.first().id.itemID)
            assertEquals(3, books.size)
            assertEquals(3, provider.bookLookups.size)
        }

    @Test
    fun `hits which cannot be resolved are skipped instead of ending the flow`() =
        runBlocking {
            val provider =
                FakeMetadataAgent(
                    hits = listOf("a", "b", "c").map { searchHit(it) },
                    resolveBook = { if (it == "a") null else testBook(it) },
                )

            val books = provider.getBookByName("a", MetadataRegion.US).toList()

            assertEquals(listOf("b", "c"), books.map { it.id.itemID })
        }

    @Test
    fun `an author shared by several hits is only looked up once`() =
        runBlocking {
            val provider =
                FakeMetadataAgent(
                    hits =
                        listOf(
                            searchHit("book-1", authors = listOf("Twain")),
                            searchHit("book-2", authors = listOf("Twain")),
                        ),
                )

            val authors = provider.getAuthorByName("Twain", MetadataRegion.US).toList()

            assertEquals(listOf("Twain"), authors.map { it.id.itemID })
            assertEquals(listOf("Twain@US"), provider.authorLookups)
        }

    @Test
    fun `the series of the hits are resolved, not the hits themselves`() =
        runBlocking {
            val provider =
                FakeMetadataAgent(hits = listOf(searchHit("book-1", series = listOf("Discworld"))))

            val series = provider.getSeriesByName("Discworld", MetadataRegion.US).toList()

            assertEquals(listOf("Discworld"), series.map { it.id.itemID })
            assertEquals(listOf("Discworld@US"), provider.seriesLookups)
            assertEquals(emptyList(), provider.bookLookups)
        }

    @Test
    fun `keywords and the author narrow the search down alongside the title`() =
        runBlocking {
            val provider = agentFor("apple")

            provider.getBookByName("apple", MetadataRegion.US, keywords = "fruit", authorName = "Twain").toList()

            assertEquals(
                listOf(SearchQuery(keywords = "fruit", title = "apple", author = "Twain")),
                provider.searchQueries,
            )
        }

    @Test
    fun `the title is what the hits are ranked against, not the order they arrived in`() =
        runBlocking {
            val provider = agentFor("zebra", "apple")

            val books = provider.getBookByName("apple", MetadataRegion.US).toList()

            assertEquals(listOf("apple", "zebra"), books.map { it.id.itemID })
        }
}
