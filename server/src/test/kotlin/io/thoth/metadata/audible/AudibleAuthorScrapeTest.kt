package io.thoth.metadata.audible

import io.ktor.client.HttpClient
import io.ktor.client.request.head
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Scrapes the live Audible pages, so it needs network access and breaks when Audible changes its markup. */
@Tag("live")
class AudibleAuthorScrapeTest {
    private val provider = AudibleMetadataAgent(imageSize = IMAGE_SIZE)
    private val rowling = "B000AP9A6K"

    @Test
    fun `serves the pages in the language of the region`() =
        runBlocking {
            // audible.de also serves the Netherlands and picks the language by the IP of the server otherwise
            val german = provider.scrapePage(AudibleRegions.DE, listOf("author", rowling))
            val japanese = provider.scrapePage(AudibleRegions.JP, listOf("author", rowling))

            assertEquals("de-DE", german?.selectFirst("html")?.attr("lang"))
            assertEquals("ja-JP", japanese?.selectFirst("html")?.attr("lang"))
        }

    @Test
    fun `scrapes name, image and biography of an author`() =
        runBlocking {
            val author = assertNotNull(provider.scrapeAuthor(AudibleRegions.US, rowling))

            assertEquals("J.K. Rowling", author.name)
            assertEquals(rowling, author.id.itemID)
            assertEquals("audible", author.id.provider)
            assertEquals("https://www.audible.com/author/$rowling", author.link)
            assertTrue(author.biography!!.length > 100, "biography was '${author.biography}'")
            assertTrue(author.imageURL!!.startsWith("https://"), "image was '${author.imageURL}'")
        }

    @Test
    fun `scrapes the biography in the language of the region`() =
        runBlocking {
            val english = assertNotNull(provider.scrapeAuthor(AudibleRegions.US, rowling)).biography
            val german = assertNotNull(provider.scrapeAuthor(AudibleRegions.DE, rowling)).biography

            assertNotNull(english)
            assertNotNull(german)
            assertNotEquals(english, german)
        }

    @Test
    fun `requests the author image in the configured resolution`() =
        runBlocking {
            val image = assertNotNull(provider.scrapeAuthor(AudibleRegions.US, rowling)).imageURL

            assertTrue(image!!.contains("_SX${IMAGE_SIZE}_"), "image was '$image'")
            HttpClient().use { client ->
                val response = client.head(image)
                assertTrue(response.status.isSuccess(), "$image answered ${response.status}")
                assertTrue(
                    response.headers[HttpHeaders.ContentType]?.startsWith("image/") == true,
                    "$image is a ${response.headers[HttpHeaders.ContentType]}",
                )
            }
        }

    @Test
    fun `returns null for an unknown author`() =
        runBlocking {
            // Audible answers an ASIN it does not know with a 404. A book ASIN cannot be used to cover the guard
            // against pages which are no author profile: /author/<book asin> redirects to the marketplace of the
            // caller, so what comes back depends on where the test runs.
            assertNull(provider.scrapeAuthor(AudibleRegions.US, "NOTANASIN"))
        }

    private companion object {
        const val IMAGE_SIZE = 900
    }
}
