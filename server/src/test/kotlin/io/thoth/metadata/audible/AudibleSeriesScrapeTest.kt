package io.thoth.metadata.audible

import io.thoth.metadata.responses.MetadataRegion
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Scrapes the live Audible pages, so it needs network access and breaks when Audible changes its markup. */
class AudibleSeriesScrapeTest {
    private val provider = AudibleMetadataProvider()
    private val dungeonCrawlerCarl = "B0937FGLYC"

    @Test
    fun `scrapes the description of a series`() =
        runBlocking {
            val description =
                assertNotNull(provider.scrapeSeriesDescription(AudibleRegions.DE, dungeonCrawlerCarl))

            assertTrue(
                description.startsWith("The apocalypse will be televised!"),
                "description was '${description.take(60)}...'",
            )
            assertFalse(description.contains("<"), "description still contains markup")
        }

    @Test
    fun `resolves a series with description and ordered books`() =
        runBlocking {
            val series = assertNotNull(provider.getSeriesByID("audible", dungeonCrawlerCarl, MetadataRegion.DE))

            assertEquals("Dungeon Crawler Carl", series.title)
            assertEquals(listOf("Matt Dinniman"), series.authors)
            assertEquals("https://www.audible.de/series/$dungeonCrawlerCarl", series.link)
            assertTrue(series.totalBooks!! >= 8, "series had ${series.totalBooks} books")
            assertTrue(
                series.description!!.startsWith("The apocalypse will be televised!"),
                "description was '${series.description?.take(60)}...'",
            )
            assertEquals(
                listOf(
                    "B08V893CH7", // 1
                    "B0934Y5S4Y", // 2
                    "B094XLNS5Z", // 3
                    "B09GD4BP6B", // 4
                    "B09ZJ7S23V", // 5
                    "B0CDXYHNB1", // 6
                    "B0DK22WZKF", // 7
                    "B0FXY3N3LF", // 8
                ),
                series.books?.take(8)?.map { it.id.itemID },
            )
            assertEquals("Dungeon Crawler Carl", series.books?.first()?.title)
        }
}
