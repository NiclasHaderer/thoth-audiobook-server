package io.thoth.server.api

import io.ktor.http.HttpStatusCode
import io.thoth.client.gen.models.AuthorCreateImpl
import io.thoth.client.gen.models.LibraryPermissionLevel
import io.thoth.client.gen.models.SeriesCreateImpl
import io.thoth.server.ThothTest
import io.thoth.server.api
import io.thoth.server.bearer
import io.thoth.server.newLibrary
import io.thoth.server.registerWithAccess
import io.thoth.server.thothServer
import kotlin.test.Test
import kotlin.test.assertEquals

class ManualCreationTest : ThothTest() {
    @Test
    fun `a hand made author is returned by the endpoint that created it`() =
        thothServer {
            val libId = newLibrary("lib")
            val token = bearer(registerWithAccess("editor", libId, LibraryPermissionLevel.READ_WRITE))

            val response = api.createAuthor(libId, AuthorCreateImpl("Terry Pratchett"), token)

            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals("Terry Pratchett", response.body().name)
            assertEquals(
                HttpStatusCode.NotFound,
                api.getAuthor(response.body().id, libId, token).status,
                "it stays hidden until a book joins it, so a later read is a miss",
            )
        }

    @Test
    fun `a hand made series is returned by the endpoint that created it`() =
        thothServer {
            val libId = newLibrary("lib")
            val token = bearer(registerWithAccess("editor", libId, LibraryPermissionLevel.READ_WRITE))

            val response = api.createSeries(libId, SeriesCreateImpl("Discworld"), token)

            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals("Discworld", response.body().title)
            assertEquals(
                HttpStatusCode.NotFound,
                api.getSeries(response.body().id, libId, token).status,
                "it stays hidden until a book joins it, so a later read is a miss",
            )
        }
}
