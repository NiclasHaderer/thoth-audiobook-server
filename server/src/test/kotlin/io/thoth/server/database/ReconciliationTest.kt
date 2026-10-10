package io.thoth.server.database

import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.thoth.client.gen.models.BookDetailedImpl
import io.thoth.client.gen.models.LibraryPermissionLevel
import io.thoth.client.gen.models.MetadataLanguage
import io.thoth.client.gen.models.NamedIdImpl
import io.thoth.client.gen.models.PlayStatus
import io.thoth.client.gen.models.TitledIdImpl
import io.thoth.server.ThothTest
import io.thoth.server.api
import io.thoth.server.bearer
import io.thoth.server.database.tables.BookAgentMetadataTable
import io.thoth.server.database.tables.BookField
import io.thoth.server.database.tables.BookFileMetadataTable
import io.thoth.server.database.tables.BookMetadataRow
import io.thoth.server.database.tables.BookUserMetadataTable
import io.thoth.server.database.tables.LibraryTable
import io.thoth.server.database.tables.MetadataLayer
import io.thoth.server.database.tables.SeriesTable
import io.thoth.server.database.tables.layer
import io.thoth.server.database.tables.reconcileLibrary
import io.thoth.server.database.tables.replaceBookAuthors
import io.thoth.server.database.tables.replaceBookSeries
import io.thoth.server.database.tables.toLibraryRow
import io.thoth.server.database.tables.update
import io.thoth.server.database.tables.write
import io.thoth.server.newAuthor
import io.thoth.server.newBook
import io.thoth.server.newLibrary
import io.thoth.server.newSeries
import io.thoth.server.registerWithAccess
import io.thoth.server.repositories.refreshSeriesDeferral
import io.thoth.server.thothServer
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import io.thoth.metadata.responses.MetadataLanguage as ServerMetadataLanguage

class ReconciliationTest : ThothTest() {
    @Test
    fun `the API hands back every field merged from the three layers`() =
        thothServer {
            val libId = newLibrary("lib", folders = listOf("/media/books"))
            val author = newAuthor("Terry Pratchett", libId)
            val series = newSeries("Discworld", libId)
            val bookId = newBook("From the tags", libId, authors = listOf(author), series = listOf(series))

            transaction {
                BookFileMetadataTable.write(
                    BookFileMetadataTable.layer(bookId).copy(
                        title = "From the tags",
                        releaseDate = LocalDate.EPOCH,
                        language = MetadataLanguage.English.toServer(),
                        narrators = listOf("Nigel Planer"),
                        genres = listOf("Fantasy"),
                        publisher = "Tagged publisher",
                        description = "Tagged description",
                    ),
                )
                BookAgentMetadataTable.write(
                    BookMetadataRow(
                        book = bookId,
                        title = "From the agent",
                        description = "Agent description",
                        isbn = "9780552131063",
                        provider = "audible",
                        providerID = "B00",
                        providerRating = 4.5f,
                    ),
                )
                BookUserMetadataTable.write(
                    BookUserMetadataTable.layer(bookId).copy(title = "Mort", publisher = "Chosen by hand"),
                )
            }

            val token = bearer(registerWithAccess("reader", libId, LibraryPermissionLevel.READONLY))
            val book = api.getBook(bookId, libId, token).body()

            assertEquals(
                BookDetailedImpl(
                    id = bookId,
                    libraryId = libId,
                    title = "Mort",
                    publisher = "Chosen by hand",
                    description = "Agent description",
                    isbn = "9780552131063",
                    provider = "audible",
                    providerID = "B00",
                    providerRating = 4.5f,
                    releaseDate = LocalDate.EPOCH,
                    language = MetadataLanguage.English,
                    narrators = listOf("Nigel Planer"),
                    genres = listOf("Fantasy"),
                    coverID = null,
                    authors = listOf(NamedIdImpl(author, "Terry Pratchett")),
                    series = listOf(TitledIdImpl(series, "Discworld")),
                    tracks = emptyList(),
                    durationMs = 0,
                    positionMs = 0,
                    status = PlayStatus.UNPLAYED,
                ),
                book,
            )
        }

    @Test
    fun `a series the agent layer replaced is gone from the API but kept in the database`() =
        thothServer {
            val libId = newLibrary("lib", folders = listOf("/media/books"))
            val tagged = newSeries("Tagged Series", libId)
            val chosen = newSeries("Chosen Series", libId)
            val bookId = newBook("Mort", libId, series = listOf(tagged))

            transaction {
                replaceBookSeries(bookId, MetadataLayer.AGENT, mapOf(chosen to null))
                BookAgentMetadataTable.write(
                    BookAgentMetadataTable.layer(bookId).copy(claimed = setOf(BookField.SERIES)),
                )
                refreshSeriesDeferral(listOf(tagged, chosen))
            }

            val token = bearer(registerWithAccess("reader", libId, LibraryPermissionLevel.READONLY))

            assertEquals(
                listOf("Chosen Series"),
                api
                    .listSeries(libId, headers = token)
                    .body()
                    .items
                    .map { it.title },
                "the series the file layer still names must not be listed",
            )
            assertEquals(
                listOf("Chosen Series"),
                api
                    .getBook(bookId, libId, token)
                    .body()
                    .series
                    .map { it.title },
                "nor hang off the book",
            )
            assertEquals(
                HttpStatusCode.NotFound,
                api.getSeries(tagged, libId, token).status,
                "nor be reachable by id",
            )
            assertEquals(
                setOf("Tagged Series", "Chosen Series"),
                transaction { SeriesTable.selectAll().map { it[SeriesTable.name] }.toSet() },
                "but it must still exist, so the file layer can claim it back",
            )
        }

    @Test
    fun `flipping the library preference re-resolves what the API returns`() =
        thothServer {
            val libId = newLibrary("lib", folders = listOf("/media/books"), preferEmbeddedMetadata = false)
            val bookId = newBook("From the tags", libId)
            transaction { BookAgentMetadataTable.write(BookMetadataRow(book = bookId, title = "From the agent")) }

            val token = bearer(registerWithAccess("reader", libId, LibraryPermissionLevel.READONLY))
            assertEquals("From the agent", api.getBook(bookId, libId, token).body().title)

            transaction {
                val library =
                    LibraryTable
                        .selectAll()
                        .where { LibraryTable.id eq libId }
                        .single()
                        .toLibraryRow()
                LibraryTable.update(library.copy(preferEmbeddedMetadata = true))
                reconcileLibrary(libId)
            }

            assertEquals(
                "From the tags",
                api.getBook(bookId, libId, token).body().title,
                "the tags win once the library prefers them",
            )
        }

    @Test
    fun `changing which layer owns the authors changes who the API lists`() =
        thothServer {
            val libId = newLibrary("lib", folders = listOf("/media/books"))
            val first = newAuthor("First", libId)
            val second = newAuthor("Second", libId)
            val bookId = newBook("Book", libId, authors = listOf(first))

            val token = bearer(registerWithAccess("reader", libId, LibraryPermissionLevel.READONLY))
            assertEquals(listOf("First"), authorNames(bookId, libId, token))

            transaction {
                replaceBookAuthors(bookId, MetadataLayer.USER, listOf(second))
                BookUserMetadataTable.write(
                    BookUserMetadataTable.layer(bookId).copy(claimed = setOf(BookField.AUTHORS)),
                )
            }

            assertEquals(listOf("Second"), authorNames(bookId, libId, token), "the user layer now owns the list")
        }

    private suspend fun ApplicationTestBuilder.authorNames(
        bookId: UUID,
        libId: UUID,
        token: Headers,
    ) = api
        .getBook(bookId, libId, token)
        .body()
        .authors
        .map { it.name }

    private fun MetadataLanguage.toServer() = ServerMetadataLanguage.valueOf(name)
}
