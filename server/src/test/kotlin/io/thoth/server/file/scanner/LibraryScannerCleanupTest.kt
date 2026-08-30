package io.thoth.server.file.scanner

import io.thoth.server.ThothTest
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.LibrariesTable
import io.thoth.server.database.tables.SeriesTable
import io.thoth.server.database.tables.TracksTable
import io.thoth.server.database.tables.create
import io.thoth.server.newAuthor
import io.thoth.server.newBook
import io.thoth.server.newLibrary
import io.thoth.server.newSeries
import io.thoth.server.newTrack
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import java.time.Instant

class LibraryScannerCleanupTest : ThothTest() {
    private val cleanup = LibraryCleanup()

    private fun newBookWithTrack(
        libraryId: UUID,
        prefix: String,
        trackScanIndex: ULong,
    ) {
        val bookAuthor = newAuthor("$prefix Author", libraryId)
        val bookSeries = newSeries("$prefix Series", libraryId)
        val newBook = newBook("$prefix Book", libraryId, authors = listOf(bookAuthor), series = listOf(bookSeries))
        newTrack(
            title = "$prefix Track",
            path = "/media/$prefix/track.mp3",
            bookId = newBook,
            libraryId = libraryId,
            scanIndex = trackScanIndex,
        )
    }

    private fun setScanIndex(
        libraryId: UUID,
        index: ULong,
    ) = transaction { LibrariesTable.update({ LibrariesTable.id eq libraryId }) { it[scanIndex] = index } }

    private fun cleanup(libraryId: UUID) {
        cleanup.removeStaleTracks(libraryId)
        cleanup.removeOrphans(libraryId)
    }

    private fun counts(libraryId: UUID) =
        transaction {
            listOf(
                TracksTable.selectAll().where { TracksTable.library eq libraryId }.count(),
                BooksTable.selectAll().where { BooksTable.library eq libraryId }.count(),
                AuthorTable.selectAll().where { AuthorTable.library eq libraryId }.count(),
                SeriesTable.selectAll().where { SeriesTable.library eq libraryId }.count(),
            )
        }

    @Test
    fun `cleanup leaves other libraries untouched`() {
        val scanned = newLibrary("scanned")
        val other = newLibrary("other")
        newBookWithTrack(scanned, "scanned", trackScanIndex = 1uL)
        newBookWithTrack(other, "other", trackScanIndex = 1uL)
        setScanIndex(scanned, 2uL)

        cleanup(scanned)

        assertEquals(
            listOf(1L, 1L, 1L, 1L),
            counts(other),
            "rescanning one library must not delete another library's track, book, author or series",
        )
    }

    @Test
    fun `cleanup removes content of the scanned library that is no longer on disk`() {
        val scanned = newLibrary("scanned")
        newBookWithTrack(scanned, "scanned", trackScanIndex = 1uL)
        setScanIndex(scanned, 2uL)

        cleanup(scanned)

        assertEquals(
            listOf(0L, 0L, 0L, 0L),
            counts(scanned),
            "a track that was not touched by the scan must be removed together with its orphaned relations",
        )
    }

    @Test
    fun `cleanup keeps content that the scan touched`() {
        val scanned = newLibrary("scanned")
        newBookWithTrack(scanned, "scanned", trackScanIndex = 2uL)
        setScanIndex(scanned, 2uL)

        cleanup(scanned)

        assertEquals(listOf(1L, 1L, 1L, 1L), counts(scanned), "a touched track and its relations must survive")
    }

    @Test
    fun `orphans within their deferDeletionUntil grace period survive cleanup`() {
        val scanned = newLibrary("scanned")
        val now = Instant.now()
        val (keptAuthor, keptSeries) =
            transaction {
                AuthorTable.create(scanned, deferDeletionUntil = now.minusMillis(1)) // expired
                SeriesTable.create(scanned, deferDeletionUntil = now.minusMillis(1))
                AuthorTable.create(scanned, deferDeletionUntil = now.plusSeconds(60)) to
                    SeriesTable.create(scanned, deferDeletionUntil = now.plusSeconds(60))
            }

        cleanup.removeOrphans(scanned)

        val (authors, series) =
            transaction {
                AuthorTable.selectAll().map { it[AuthorTable.id].value } to
                    SeriesTable.selectAll().map { it[SeriesTable.id].value }
            }
        assertEquals(listOf(keptAuthor), authors, "only the author inside the grace period must survive")
        assertEquals(listOf(keptSeries), series, "only the series inside the grace period must survive")
    }

    @Test
    fun `pruneOrphans never deletes a track`() {
        val scanned = newLibrary("scanned")
        newBookWithTrack(scanned, "scanned", trackScanIndex = 1uL)
        setScanIndex(scanned, 2uL)

        cleanup.removeOrphans(scanned)

        assertEquals(
            listOf(1L, 1L, 1L, 1L),
            counts(scanned),
            "pruneOrphans runs outside a scan, so it must not act on scanIndex staleness",
        )
    }
}
