package io.thoth.server.api

import io.thoth.client.gen.models.LibraryPermissionLevel
import io.thoth.server.ThothTest
import io.thoth.server.api
import io.thoth.server.bearer
import io.thoth.server.newBook
import io.thoth.server.newLibrary
import io.thoth.server.newTrack
import io.thoth.server.registerWithAccess
import io.thoth.server.thothServer
import kotlin.test.Test
import kotlin.test.assertEquals

class TrackNumberingTest : ThothTest() {
    @Test
    fun `books without trusted numbering are numbered by position`() =
        thothServer {
            val libId = newLibrary("lib", folders = listOf("/media/books"))
            val bookId = newBook("Dune", libId)
            newTrack("Chapter 10", "/media/books/dune/Chapter 10.mp3", bookId, libId, trackNr = 1)
            newTrack("Chapter 02", "/media/books/dune/Chapter 02.mp3", bookId, libId)
            val token = bearer(registerWithAccess("listener", libId, LibraryPermissionLevel.READONLY))

            val tracks = api.getBook(bookId, libId, token).body().tracks
            assertEquals(listOf("Chapter 02" to 1, "Chapter 10" to 2), tracks.map { it.title to it.trackNr })
        }
}
