package io.thoth.server.file.scanner

import io.thoth.models.FileScanner
import io.thoth.models.NamedMetadataAgent
import io.thoth.server.ThothTest
import io.thoth.server.database.tables.AuthorEntity
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.BookEntity
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.LibraryEntity
import io.thoth.server.database.tables.SeriesEntity
import io.thoth.server.database.tables.SeriesTable
import io.thoth.server.database.tables.TrackEntity
import io.thoth.server.database.tables.TracksTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SizedCollection
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryScannerCleanupTest : ThothTest() {
    private val cleanup = LibraryCleanup()

    private fun newLibrary(libraryName: String): UUID =
        transaction {
            LibraryEntity
                .new {
                    name = libraryName
                    folders = listOf("/media/$libraryName")
                    metadataAgents = listOf(NamedMetadataAgent("audible"))
                    fileScanners = listOf(FileScanner("AudioFolderScanner"))
                    language = "en"
                }.id
                .value
        }

    private fun newBookWithTrack(
        libraryId: UUID,
        prefix: String,
        trackScanIndex: ULong,
    ) = transaction {
        val lib = LibraryEntity[libraryId]
        val bookAuthor =
            AuthorEntity.new {
                name = "$prefix Author"
                library = lib
            }
        val bookSeries =
            SeriesEntity.new {
                title = "$prefix Series"
                library = lib
            }
        val newBook =
            BookEntity.new {
                title = "$prefix Book"
                library = lib
                authors = SizedCollection(listOf(bookAuthor))
                series = SizedCollection(listOf(bookSeries))
            }
        TrackEntity.new {
            title = "$prefix Track"
            path = "/media/$prefix/track.mp3"
            duration = 1
            accessTime = 0
            scanIndex = trackScanIndex
            book = newBook
            library = lib
        }
    }

    private fun cleanup(libraryId: UUID) {
        cleanup.removeStaleTracks(libraryId)
        cleanup.removeOrphans(libraryId)
    }

    private fun counts(libraryId: UUID) =
        transaction {
            listOf(
                TrackEntity.find { TracksTable.library eq libraryId }.count(),
                BookEntity.find { BooksTable.library eq libraryId }.count(),
                AuthorEntity.find { AuthorTable.library eq libraryId }.count(),
                SeriesEntity.find { SeriesTable.library eq libraryId }.count(),
            )
        }

    @Test
    fun `cleanup leaves other libraries untouched`() {
        val scanned = newLibrary("scanned")
        val other = newLibrary("other")
        newBookWithTrack(scanned, "scanned", trackScanIndex = 1uL)
        newBookWithTrack(other, "other", trackScanIndex = 1uL)
        transaction { LibraryEntity[scanned].scanIndex = 2uL }

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
        transaction { LibraryEntity[scanned].scanIndex = 2uL }

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
        transaction { LibraryEntity[scanned].scanIndex = 2uL }

        cleanup(scanned)

        assertEquals(listOf(1L, 1L, 1L, 1L), counts(scanned), "a touched track and its relations must survive")
    }

    @Test
    fun `pruneOrphans never deletes a track`() {
        val scanned = newLibrary("scanned")
        newBookWithTrack(scanned, "scanned", trackScanIndex = 1uL)
        transaction { LibraryEntity[scanned].scanIndex = 2uL }

        cleanup.removeOrphans(scanned)

        assertEquals(
            listOf(1L, 1L, 1L, 1L),
            counts(scanned),
            "pruneOrphans runs outside a scan, so it must not act on scanIndex staleness",
        )
    }
}
