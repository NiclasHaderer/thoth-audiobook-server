package io.thoth.server.api

import io.ktor.http.HttpStatusCode
import io.thoth.client.gen.models.LibraryPermissionLevel
import io.thoth.client.gen.models.ProgressUpdateImpl
import io.thoth.server.ThothTest
import io.thoth.server.api
import io.thoth.server.bearer
import io.thoth.server.grant
import io.thoth.server.newBook
import io.thoth.server.newLibrary
import io.thoth.server.newTrack
import io.thoth.server.registerWithAccess
import io.thoth.server.revoke
import io.thoth.server.thothServer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Access is resolved from the database on every request rather than baked into the JWT, so revoking
 * a library has to take effect immediately - even while the user still holds a valid access token.
 */
class LibraryRevocationTest : ThothTest() {
    @Test
    fun `revoking a library hides its books, progress and history from the same token`() =
        thothServer {
            val libId = newLibrary("lib", folders = listOf("/media/books"))
            val bookId = newBook("Dune", libId)
            val trackId = newTrack("t1", "/media/books/dune/1.mp3", bookId, libId, trackNr = 1)
            repeat(2) { newTrack("t${it + 2}", "/media/books/dune/${it + 2}.mp3", bookId, libId, trackNr = it + 2) }
            val token = bearer(registerWithAccess("listener", libId, LibraryPermissionLevel.READONLY))

            // Build up some state while access is still granted
            assertEquals(
                HttpStatusCode.NoContent,
                api.setBookProgress(bookId, libId, ProgressUpdateImpl(positionMs = 42000), token).status,
            )
            assertTrue(
                api
                    .listBooks(libId, headers = token)
                    .body()
                    .items
                    .any { it.title == "Dune" },
            )
            assertTrue(api.getContinueListening(headers = token).body().any { it.title == "Dune" })
            assertTrue(
                api
                    .getListeningHistory(headers = token)
                    .body()
                    .items
                    .any { it.book.title == "Dune" },
            )

            revoke("listener", libId)

            // Same token, no new login
            assertEquals(HttpStatusCode.Forbidden, api.listBooks(libId, headers = token).status)
            assertEquals(HttpStatusCode.Forbidden, api.getBook(bookId, libId, token).status)
            assertEquals(HttpStatusCode.Forbidden, api.getLibrary(libId, token).status)
            assertEquals(HttpStatusCode.Forbidden, api.getAudioFile(trackId, token).status)

            assertTrue(api.getContinueListening(headers = token).body().isEmpty())
            assertTrue(
                api
                    .getListeningHistory(headers = token)
                    .body()
                    .items
                    .isEmpty(),
            )
            assertTrue(api.listLibraries(token).body().isEmpty(), "the library itself must not be listed")

            val search = api.searchInAllLibraries(q = "Dune", headers = token).body()
            assertTrue(search.books.isEmpty(), "revoked books must not surface in search: $search")

            assertEquals(
                HttpStatusCode.Forbidden,
                api.setBookProgress(bookId, libId, ProgressUpdateImpl(positionMs = 99000), token).status,
            )
        }

    @Test
    fun `regranting a library brings the old progress back`() =
        thothServer {
            val libId = newLibrary("lib", folders = listOf("/media/books"))
            val bookId = newBook("Dune", libId)
            repeat(3) { newTrack("t$it", "/media/books/dune/$it.mp3", bookId, libId, trackNr = it + 1) }
            val token = bearer(registerWithAccess("listener", libId, LibraryPermissionLevel.READONLY))

            api.setBookProgress(bookId, libId, ProgressUpdateImpl(positionMs = 42000), token)

            revoke("listener", libId)
            assertTrue(api.getContinueListening(headers = token).body().isEmpty())

            grant("listener", libId, LibraryPermissionLevel.READONLY)
            assertEquals(
                42000,
                api
                    .getContinueListening(headers = token)
                    .body()
                    .single()
                    .positionMs,
            )
        }
}
