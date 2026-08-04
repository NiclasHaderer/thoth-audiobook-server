package io.thoth.server.database.access

import io.thoth.models.FileScanner
import io.thoth.models.NamedMetadataAgent
import io.thoth.server.ThothTest
import io.thoth.server.database.tables.BookEntity
import io.thoth.server.database.tables.LibraryEntity
import io.thoth.server.database.tables.TrackEntity
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TracksAccessTest : ThothTest() {
    private fun newLibrary(
        libraryName: String,
        libraryScanIndex: ULong = 1uL,
    ): UUID =
        transaction {
            LibraryEntity
                .new {
                    name = libraryName
                    folders = listOf("/media/$libraryName")
                    metadataAgents = listOf(NamedMetadataAgent("audible"))
                    fileScanners = listOf(FileScanner("AudioFolderScanner"))
                    language = "en"
                    scanIndex = libraryScanIndex
                }.id
                .value
        }

    private fun newTrack(
        trackLibrary: UUID,
        bookLibrary: UUID = trackLibrary,
        fileModifiedAt: Long = 1000,
    ): UUID =
        transaction {
            val book =
                BookEntity.new {
                    title = "A Book"
                    library = LibraryEntity[bookLibrary]
                }
            TrackEntity
                .new {
                    title = "A Track"
                    path = "/media/track-${UUID.randomUUID()}.mp3"
                    duration = 1
                    accessTime = fileModifiedAt
                    scanIndex = 0uL
                    this.book = book
                    library = LibraryEntity[trackLibrary]
                }.id
                .value
        }

    @Test
    fun `an unchanged file has not been updated`() {
        val track = newTrack(newLibrary("lib"), fileModifiedAt = 1000)

        assertFalse(transaction { TrackEntity[track].hasBeenUpdated(1000) })
    }

    @Test
    fun `a file with a newer mtime has been updated`() {
        val track = newTrack(newLibrary("lib"), fileModifiedAt = 1000)

        assertTrue(transaction { TrackEntity[track].hasBeenUpdated(2000) })
    }

    @Test
    fun `markAsTouched stamps the scan index of the track's own library`() {
        val trackLibrary = newLibrary("tracks", libraryScanIndex = 7uL)
        val bookLibrary = newLibrary("books", libraryScanIndex = 99uL)
        val track = newTrack(trackLibrary, bookLibrary = bookLibrary)

        transaction { TrackEntity[track].markAsTouched() }

        assertEquals(7uL, transaction { TrackEntity[track].scanIndex })
    }
}
