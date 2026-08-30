package io.thoth.server.api

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.thoth.models.LibraryPermissionLevel
import io.thoth.server.ThothTest
import io.thoth.server.bodyOf
import io.thoth.server.grant
import io.thoth.server.newBook
import io.thoth.server.newLibrary
import io.thoth.server.newTrack
import io.thoth.server.registerWithAccess
import io.thoth.server.revoke
import io.thoth.server.statusOf
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
        thothServer { client ->
            val libId = newLibrary("lib", folders = listOf("/media/books"))
            val bookId = newBook("Dune", libId)
            val trackId = newTrack("t1", "/media/books/dune/1.mp3", bookId, libId, trackNr = 1)
            repeat(2) { newTrack("t${it + 2}", "/media/books/dune/${it + 2}.mp3", bookId, libId, trackNr = it + 2) }
            val token = client.registerWithAccess("listener", libId, LibraryPermissionLevel.READONLY)

            // Build up some state while access is still granted
            assertEquals(
                HttpStatusCode.NoContent,
                client.put("/api/libraries/$libId/books/$bookId/progress") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody("{\"positionMs\": 42000}")
                }.status,
            )
            assertTrue(client.bodyOf(token, "/api/libraries/$libId/books").contains("Dune"))
            assertTrue(client.bodyOf(token, "/api/me/continue-listening").contains("Dune"))
            assertTrue(client.bodyOf(token, "/api/me/history").contains("Dune"))

            revoke("listener", libId)

            // Same token, no new login
            assertEquals(HttpStatusCode.Forbidden, client.statusOf(token, "/api/libraries/$libId/books"))
            assertEquals(HttpStatusCode.Forbidden, client.statusOf(token, "/api/libraries/$libId/books/$bookId"))
            assertEquals(HttpStatusCode.Forbidden, client.statusOf(token, "/api/libraries/$libId"))
            assertEquals(HttpStatusCode.Forbidden, client.statusOf(token, "/api/stream/audio/$trackId"))

            assertEquals("[]", client.bodyOf(token, "/api/me/continue-listening"))
            assertTrue(client.bodyOf(token, "/api/me/history").contains("\"items\":[]"))
            assertEquals("[]", client.bodyOf(token, "/api/libraries"), "the library itself must not be listed")

            val search = client.bodyOf(token, "/api/libraries/search?q=Dune")
            assertTrue(!search.contains("Dune"), "revoked books must not surface in search: $search")

            assertEquals(
                HttpStatusCode.Forbidden,
                client.put("/api/libraries/$libId/books/$bookId/progress") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody("{\"positionMs\": 99000}")
                }.status,
            )
        }

    @Test
    fun `regranting a library brings the old progress back`() =
        thothServer { client ->
            val libId = newLibrary("lib", folders = listOf("/media/books"))
            val bookId = newBook("Dune", libId)
            repeat(3) { newTrack("t$it", "/media/books/dune/$it.mp3", bookId, libId, trackNr = it + 1) }
            val token = client.registerWithAccess("listener", libId, LibraryPermissionLevel.READONLY)

            client.put("/api/libraries/$libId/books/$bookId/progress") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody("{\"positionMs\": 42000}")
            }

            revoke("listener", libId)
            assertEquals("[]", client.bodyOf(token, "/api/me/continue-listening"))

            grant("listener", libId, LibraryPermissionLevel.READONLY)
            assertTrue(client.bodyOf(token, "/api/me/continue-listening").contains("42000"))
        }
}
