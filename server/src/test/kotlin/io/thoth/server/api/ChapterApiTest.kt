package io.thoth.server.api

import io.ktor.http.HttpStatusCode
import io.thoth.client.gen.models.BookField
import io.thoth.client.gen.models.BookUpdateImpl
import io.thoth.client.gen.models.ChapterMarkImpl
import io.thoth.openapi.common.Patch
import io.thoth.server.ThothTest
import io.thoth.server.api
import io.thoth.server.bearer
import io.thoth.server.database.tables.TrackChapter
import io.thoth.server.login
import io.thoth.server.newBook
import io.thoth.server.newLibrary
import io.thoth.server.newTrack
import io.thoth.server.thothServer
import kotlin.test.Test
import kotlin.test.assertEquals

class ChapterApiTest : ThothTest() {
    @Test
    fun `chapters come from the embedded markers`() =
        thothServer {
            val libId = newLibrary("lib", folders = listOf("/media/books"))
            val bookId = newBook("Dune", libId)
            val trackId =
                newTrack(
                    "Dune",
                    "/media/books/dune/Dune.m4b",
                    bookId,
                    libId,
                    durationMs = 4000,
                    chapters = listOf(TrackChapter("One", 0), TrackChapter("Two", 2000)),
                )
            val token = bearer(login("admin"))

            val chapters = api.getBook(bookId, libId, token).body().chapters

            assertEquals(listOf("One" to 0L, "Two" to 2000L), chapters.map { it.title to it.startMs })
            assertEquals(listOf(2000L, 4000L), chapters.map { it.endMs })
            assertEquals(listOf(trackId, trackId), chapters.map { it.trackId })
        }

    @Test
    fun `edited chapters override the files until they are reset`() =
        thothServer {
            val libId = newLibrary("lib", folders = listOf("/media/books"))
            val bookId = newBook("Dune", libId)
            newTrack("Part 1", "/media/books/dune/Part 1.mp3", bookId, libId, trackNr = 1, durationMs = 1000)
            newTrack("Part 2", "/media/books/dune/Part 2.mp3", bookId, libId, trackNr = 2, durationMs = 1000)
            val token = bearer(login("admin"))
            val marks =
                listOf(
                    ChapterMarkImpl(startMs = 0, title = "Prologue"),
                    ChapterMarkImpl(startMs = 500, title = "Arrakis"),
                )

            val response = api.updateBook(bookId, libId, BookUpdateImpl(chapters = Patch.Set(marks)), token)

            assertEquals(HttpStatusCode.OK, response.status)
            val edited = api.getBook(bookId, libId, token).body()
            assertEquals(listOf("Prologue", "Arrakis"), edited.chapters.map { it.title })
            assertEquals(listOf(500L, 2000L), edited.chapters.map { it.endMs })
            assertEquals(listOf(BookField.CHAPTERS), edited.locked)

            api.updateBook(bookId, libId, BookUpdateImpl(unlock = Patch.Set(listOf(BookField.CHAPTERS))), token)

            val unlocked = api.getBook(bookId, libId, token).body()
            assertEquals(listOf("Part 1", "Part 2"), unlocked.chapters.map { it.title })
            assertEquals(emptyList(), unlocked.locked)
        }

    @Test
    fun `edited chapters can be left untitled`() =
        thothServer {
            val libId = newLibrary("lib", folders = listOf("/media/books"))
            val bookId = newBook("Dune", libId)
            newTrack("Part 1", "/media/books/dune/Part 1.mp3", bookId, libId, durationMs = 1000)
            val token = bearer(login("admin"))
            val marks =
                listOf(
                    ChapterMarkImpl(startMs = 0, title = "Prologue"),
                    ChapterMarkImpl(startMs = 500, title = " "),
                )

            val response = api.updateBook(bookId, libId, BookUpdateImpl(chapters = Patch.Set(marks)), token)

            assertEquals(HttpStatusCode.OK, response.status)
            val edited = api.getBook(bookId, libId, token).body()
            assertEquals(listOf("Prologue", " "), edited.chapters.map { it.title })
        }

    @Test
    fun `invalid chapters are rejected`() =
        thothServer {
            val libId = newLibrary("lib", folders = listOf("/media/books"))
            val bookId = newBook("Dune", libId)
            newTrack("Part 1", "/media/books/dune/Part 1.mp3", bookId, libId, durationMs = 1000)
            val token = bearer(login("admin"))

            val invalid =
                listOf(
                    emptyList(),
                    listOf(ChapterMarkImpl(startMs = 500, title = "B"), ChapterMarkImpl(startMs = 0, title = "A")),
                    listOf(
                        ChapterMarkImpl(startMs = 0, title = "A"),
                        ChapterMarkImpl(startMs = 1000, title = "Past the end"),
                    ),
                )
            for (marks in invalid) {
                val response = api.updateBook(bookId, libId, BookUpdateImpl(chapters = Patch.Set(marks)), token)
                assertEquals(HttpStatusCode.BadRequest, response.status, "$marks must be rejected")
            }
        }
}
