package io.thoth.server.api

import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.thoth.client.gen.models.BookUpdateImpl
import io.thoth.openapi.common.Patch
import io.thoth.server.ThothTest
import io.thoth.server.api
import io.thoth.server.bearer
import io.thoth.server.database.tables.BookFileMetadataTable
import io.thoth.server.database.tables.layer
import io.thoth.server.database.tables.write
import io.thoth.server.login
import io.thoth.server.newBook
import io.thoth.server.newLibrary
import io.thoth.server.thothServer
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BookPatchTest : ThothTest() {
    @Test
    fun `an explicit null blanks a field while a missing key leaves it alone`() =
        thothServer {
            val libId = newLibrary("lib", folders = listOf("/media/books"))
            val bookId = newBook("Dune", libId)
            transaction {
                BookFileMetadataTable.write(
                    BookFileMetadataTable.layer(bookId).copy(description = "From the tags", publisher = "Tagged"),
                )
            }
            val token = bearer(login("admin"))

            val response = api.updateBook(bookId, libId, BookUpdateImpl(description = Patch.Set(null)), token)

            assertEquals(HttpStatusCode.OK, response.status)
            val book = response.body()
            assertNull(book.description, "null must blank the tagged description")
            assertEquals("Tagged", book.publisher, "a key that was not sent must not change anything")
            assertEquals("Dune", book.title, "an unsent title must not change anything")
        }

    @Test
    fun `a blank title is rejected`() =
        thothServer {
            val libId = newLibrary("lib", folders = listOf("/media/books"))
            val bookId = newBook("Dune", libId)
            val token = bearer(login("admin"))

            val response = api.updateBook(bookId, libId, BookUpdateImpl(title = Patch.Set("  ")), token)

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    @Test
    fun `a null title is rejected`() =
        thothServer {
            val libId = newLibrary("lib", folders = listOf("/media/books"))
            val bookId = newBook("Dune", libId)
            val token = bearer(login("admin"))

            val response =
                client.patch("/api/libraries/$libId/books/$bookId") {
                    headers.appendAll(token)
                    contentType(ContentType.Application.Json)
                    setBody("""{"title": null}""")
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
}
