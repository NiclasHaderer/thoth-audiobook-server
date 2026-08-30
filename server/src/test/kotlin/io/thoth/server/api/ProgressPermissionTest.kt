package io.thoth.server.api

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.patch
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.thoth.models.LibraryPermissionLevel
import io.thoth.server.ThothTest
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
        withLibrary { client, libId, bookId ->
            val token = client.registerWithAccess("listener", libId, LibraryPermissionLevel.READONLY)

            assertEquals(
                HttpStatusCode.NoContent,
                client.putProgress(token, libId, bookId).status,
                "a readonly member owns their own progress",
            )
            assertEquals(
                HttpStatusCode.Forbidden,
                client.renameBook(token, libId, bookId).status,
                "library content is still off limits",
            )
        }

    @Test
    fun `a read write member can do both`() =
        withLibrary { client, libId, bookId ->
            val token = client.registerWithAccess("curator", libId, LibraryPermissionLevel.READ_WRITE)

            assertEquals(HttpStatusCode.NoContent, client.putProgress(token, libId, bookId).status)
            assertEquals(HttpStatusCode.OK, client.renameBook(token, libId, bookId).status)
        }

    private fun withLibrary(block: suspend (HttpClient, UUID, UUID) -> Unit) =
        thothServer { client ->
            val libId = newLibrary("lib", folders = listOf("/media/books"))
            val bookId = newBook("Dune", libId)
            newTrack("t1", "/media/books/dune/1.mp3", bookId, libId, trackNr = 1)
            block(client, libId, bookId)
        }

    private suspend fun HttpClient.putProgress(
        token: String,
        libId: UUID,
        bookId: UUID,
    ) = put("/api/libraries/$libId/books/$bookId/progress") {
        bearerAuth(token)
        contentType(ContentType.Application.Json)
        setBody("{\"positionMs\": 42000}")
    }

    private suspend fun HttpClient.renameBook(
        token: String,
        libId: UUID,
        bookId: UUID,
    ) = patch("/api/libraries/$libId/books/$bookId") {
        bearerAuth(token)
        contentType(ContentType.Application.Json)
        setBody("{\"title\": \"Renamed\"}")
    }
}
