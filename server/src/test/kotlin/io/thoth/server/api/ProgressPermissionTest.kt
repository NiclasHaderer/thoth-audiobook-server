package io.thoth.server.api

import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.thoth.client.gen.models.BookUpdateImpl
import io.thoth.client.gen.models.LibraryPermissionLevel
import io.thoth.client.gen.models.ProgressUpdateImpl
import io.thoth.server.ThothTest
import io.thoth.server.api
import io.thoth.server.bearer
import io.thoth.server.newBook
import io.thoth.server.newLibrary
import io.thoth.server.newTrack
import io.thoth.server.registerWithAccess
import io.thoth.server.thothServer
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Progress routes are marked [io.thoth.server.plugins.auth.UserScoped], which makes
 * assertLibraryPermissions treat their PUTs as reads. That only holds at the route layer, so it has
 * to be checked over HTTP rather than against the repository.
 */
class ProgressPermissionTest : ThothTest() {
    @Test
    fun `a readonly member can record progress but still cannot edit the library`() =
        withLibrary(LibraryPermissionLevel.READONLY) { token, libId, bookId ->
            assertEquals(
                HttpStatusCode.NoContent,
                api.setBookProgress(bookId, libId, ProgressUpdateImpl(positionMs = 42000), token).status,
                "a readonly member owns their own progress",
            )
            assertEquals(
                HttpStatusCode.Forbidden,
                api.updateBook(bookId, libId, BookUpdateImpl(title = "Renamed"), token).status,
                "library content is still off limits",
            )
        }

    @Test
    fun `a read write member can do both`() =
        withLibrary(LibraryPermissionLevel.READ_WRITE) { token, libId, bookId ->
            assertEquals(
                HttpStatusCode.NoContent,
                api.setBookProgress(bookId, libId, ProgressUpdateImpl(positionMs = 42000), token).status,
            )
            assertEquals(
                HttpStatusCode.OK,
                api.updateBook(bookId, libId, BookUpdateImpl(title = "Renamed"), token).status,
            )
        }

    private fun withLibrary(
        level: LibraryPermissionLevel,
        block: suspend ApplicationTestBuilder.(Headers, UUID, UUID) -> Unit,
    ) = thothServer {
        val libId = newLibrary("lib", folders = listOf("/media/books"))
        val bookId = newBook("Dune", libId)
        newTrack("t1", "/media/books/dune/1.mp3", bookId, libId, trackNr = 1)
        block(bearer(registerWithAccess("listener", libId, level)), libId, bookId)
    }
}
