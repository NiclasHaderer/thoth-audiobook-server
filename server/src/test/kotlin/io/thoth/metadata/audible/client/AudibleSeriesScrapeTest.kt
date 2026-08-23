package io.thoth.metadata.audible.client

import io.thoth.metadata.audible.models.AudibleRegions
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Scrapes the live Audible pages, so it needs network access and breaks when Audible changes its markup. */
class AudibleSeriesScrapeTest {
    private val dungeonCrawlerCarl = "B0937FGLYC"

    @Test
    fun `scrapes the description of a series`() =
        runBlocking {
            val description =
                assertNotNull(getAudibleSeriesDescription(AudibleRegions.DE, dungeonCrawlerCarl))

            assertTrue(
                description.startsWith("The apocalypse will be televised!"),
                "description was '${description.take(60)}...'",
            )
            assertFalse(description.contains("<"), "description still contains markup")
        }

    @Test
    fun `resolves a series with description and ordered books`() =
        runBlocking {
            val series = assertNotNull(getAudibleSeries(AudibleRegions.DE, 500, dungeonCrawlerCarl))

            assertEquals("Dungeon Crawler Carl", series.title)
            assertEquals(listOf("Matt Dinniman"), series.authors)
            assertEquals("https://www.audible.de/series/$dungeonCrawlerCarl", series.link)
            assertTrue(series.totalBooks!! >= 8, "series had ${series.totalBooks} books")
            assertTrue(
                series.description!!.startsWith("The apocalypse will be televised!"),
                "description was '${series.description?.take(60)}...'",
            )
            assertEquals("B08V893CH7", series.books?.first()?.id?.itemID)
            assertEquals("Dungeon Crawler Carl", series.books?.first()?.title)
        }
}
