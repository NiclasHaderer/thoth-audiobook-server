package io.thoth.metadata

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MetadataAgentWrapperTest {
    private val audible = FakeMetadataProvider(name = "audible", hits = listOf(searchHit("a", provider = "audible")))
    private val openLibrary =
        FakeMetadataProvider(name = "openLibrary", hits = listOf(searchHit("b", provider = "openLibrary")))

    private fun wrapper() =
        MetadataAgentWrapper(listOf(SearchBasedMetadataAgent(audible), SearchBasedMetadataAgent(openLibrary)))

    @Test
    fun `an ID lookup only goes to the agent it belongs to`() =
        runBlocking {
            wrapper().getBookByID(providerId = "openLibrary", bookId = "b", region = "US")

            assertEquals(emptyList(), audible.bookLookups)
            assertEquals(listOf("b@US"), openLibrary.bookLookups)
        }

    @Test
    fun `an ID of an unknown provider has no result`() =
        runBlocking {
            assertNull(wrapper().getBookByID(providerId = "goodreads", bookId = "b", region = "US"))
        }

    @Test
    fun `a search asks every agent`() =
        runBlocking {
            val found = wrapper().search(region = "US", title = "a")

            assertEquals(listOf("a", "b"), found.map { it.id.itemID })
        }

    @Test
    fun `a lookup by name hands out the results of the first agent before touching the second`() =
        runBlocking {
            val first = wrapper().getBookByName("a", "US").first()

            assertEquals("a", first.id.itemID)
            assertEquals(0, openLibrary.searchCalls.get())
        }

    @Test
    fun `collecting a lookup by name walks through all agents`() =
        runBlocking {
            val books = wrapper().getBookByName("a", "US").toList()

            assertEquals(listOf("a", "b"), books.map { it.id.itemID })
        }
}
