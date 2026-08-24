package io.thoth.metadata.audiobookdb

import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Queries the live AudiobookDB API, so it needs network access and breaks when the API changes. */
class AudiobookDbApiLiveTest {
    private val provider = AudiobookDbMetadataProvider()
    private val projectHailMary = "hPjDhje6pNjQ"
    private val andyWeir = "ypMPwXzWXaiX"
    private val forwardCollection = "rtEwTXFwaiHr"

    @Test
    fun `maps a book`() =
        runBlocking {
            val book = assertNotNull(provider.getBookByID("audiobookdb", projectHailMary, "US"))

            assertEquals(projectHailMary, book.id.itemID)
            assertEquals("audiobookdb", book.id.provider)
            assertEquals("Project Hail Mary", book.title)
            assertEquals("https://audiobookdb.org/books/$projectHailMary", book.link)
            assertEquals(listOf("Andy Weir"), book.authors?.map { it.name })
            assertEquals(listOf(andyWeir), book.authors?.map { it.id.itemID })
            assertEquals("https://audiobookdb.org/people/$andyWeir", book.authors?.first()?.link)
            assertEquals(listOf("Ray Porter"), book.narrators)
            assertEquals(LocalDate.of(2021, 5, 4), book.releaseDate)
            assertEquals("Audible Studios", book.publisher)
            assertEquals("english", book.language)
            assertEquals("9781603935470", book.isbn)
            assertTrue(book.providerRating!! > 0f, "rating was ${book.providerRating}")
            assertTrue(book.coverURL!!.startsWith("https://"), "cover was '${book.coverURL}'")
            assertTrue(book.description!!.startsWith("Ryland Grace"), "description was '${book.description?.take(60)}...'")
        }

    @Test
    fun `searches for audiobooks`() =
        runBlocking {
            val hits = provider.search("US", keywords = "project hail mary")

            val book = assertNotNull(hits.find { it.id.itemID == projectHailMary }, "hits were ${hits.map { it.title }}")
            assertEquals("Project Hail Mary", book.title)
            assertEquals(listOf("Andy Weir"), book.authors?.map { it.name })
            assertTrue(book.coverURL!!.startsWith("https://"), "cover was '${book.coverURL}'")
        }

    @Test
    fun `looks up an author`() =
        runBlocking {
            val author = assertNotNull(provider.getAuthorByID("audiobookdb", andyWeir, "US"))

            assertEquals("Andy Weir", author.name)
            assertEquals(andyWeir, author.id.itemID)
            assertEquals("audiobookdb", author.id.provider)
            assertEquals("https://audiobookdb.org/people/$andyWeir", author.link)
            assertEquals(LocalDate.of(1972, 6, 16), author.birthDate)
            assertEquals("Davis, CA", author.bornIn)
            assertTrue(!author.biography.isNullOrBlank(), "biography was '${author.biography}'")
        }

    @Test
    fun `resolves a series with its books`() =
        runBlocking {
            val series = assertNotNull(provider.getSeriesByID("audiobookdb", forwardCollection, "US"))

            assertEquals("Forward Collection", series.title)
            assertEquals("https://audiobookdb.org/series/$forwardCollection", series.link)
            val books = assertNotNull(series.books)
            assertTrue(books.size >= 6, "series only resolved ${books.size} books")
            assertTrue(books.all { !it.title.isNullOrBlank() }, "series contained untitled books")
        }

    @Test
    fun `returns null for unknown ids`() =
        runBlocking {
            assertNull(provider.getBookByID("audiobookdb", "zzzzzzzzzzzz", "US"))
            assertNull(provider.getSeriesByID("audiobookdb", "zzzzzzzzzzzz", "US"))
            assertNull(provider.getAuthorByID("audiobookdb", "zzzzzzzzzzzz", "US"))
        }
}
