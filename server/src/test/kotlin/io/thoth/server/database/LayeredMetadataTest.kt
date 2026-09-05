package io.thoth.server.database

import io.thoth.models.BookUpdate
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.ThothTest
import io.thoth.server.database.tables.AuthorBookTable
import io.thoth.server.database.tables.BookAgentMetadataTable
import io.thoth.server.database.tables.BookFileMetadataTable
import io.thoth.server.database.tables.BookMetadata
import io.thoth.server.database.tables.BookMetadataRow
import io.thoth.server.database.tables.BookUserMetadataTable
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.MetadataLayer
import io.thoth.server.database.tables.SeriesBookTable
import io.thoth.server.database.tables.SeriesTable
import io.thoth.server.database.tables.TracksTable
import io.thoth.server.database.tables.layer
import io.thoth.server.database.tables.replaceBookAuthors
import io.thoth.server.database.tables.write
import io.thoth.server.database.views.AuthorMetadataView
import io.thoth.server.database.views.BookMetadataView
import io.thoth.server.file.TrackManager
import io.thoth.server.file.analyzer.AudioFileAnalysisResultImpl
import io.thoth.server.file.scanner.LibraryCleanup
import io.thoth.server.newAuthor
import io.thoth.server.newBook
import io.thoth.server.newLibrary
import io.thoth.server.newSeries
import io.thoth.server.newUser
import io.thoth.server.repositories.AuthorRepository
import io.thoth.server.repositories.BookRepository
import io.thoth.server.repositories.DEFER_DELETION_GRACE
import io.thoth.server.repositories.SeriesRepository
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.mp.KoinPlatform.getKoin
import java.time.Instant
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LayeredMetadataTest : ThothTest() {
    private val bookRepository by lazy { getKoin().get<BookRepository>() }
    private val authorRepository by lazy { getKoin().get<AuthorRepository>() }
    private val trackManager by lazy { getKoin().get<TrackManager>() }

    private var libId: UUID = UUID.randomUUID()
    private var userId: UUID = UUID.randomUUID()

    @BeforeTest
    fun createLibrary() {
        libId = newLibrary("lib", folders = listOf("/media/books"))
        userId = newUser("test-user", admin = true)
    }

    private fun scan(
        authors: List<String> = listOf("An Author"),
        description: String? = null,
        narrators: List<String> = emptyList(),
        series: String? = null,
        book: String = "A Book",
    ) = AudioFileAnalysisResultImpl(
        title = "Chapter 1",
        authors = authors,
        book = book,
        series = series,
        durationMs = 60_000,
        path = "/media/books/$book/01.mp3",
        lastModified = Instant.EPOCH,
        description = description,
        narrators = narrators,
    )

    private fun bookId() = transaction { bookRepository.findByTaggedName("A Book", emptyList(), libId)!!.id }

    @Test
    fun `a rescan keeps an edited field but still picks up the retagged ones`() {
        trackManager.insert(scan(description = "From the tags", narrators = listOf("First")), libId)
        val id = bookId()

        bookRepository.modify(userId, id, libId, bookUpdate(description = "Mine"))
        trackManager.insert(scan(description = "Retagged", narrators = listOf("Second")), libId)

        val book = bookRepository.raw(id, libId)
        assertEquals("Mine", book.description, "a rescan must not overwrite an edit")
        assertEquals(listOf("Second"), book.narrators, "a field nobody edited must follow the tags")
    }

    @Test
    fun `clearing the user value falls back to the layer underneath`() {
        val id = newBook("A Book", libId)
        transaction {
            BookFileMetadataTable.write(BookFileMetadataTable.layer(id).copy(description = "From the tags"))
            BookUserMetadataTable.write(BookMetadataRow(book = id, description = "Mine"))
        }
        assertEquals("Mine", bookRepository.raw(id, libId).description)

        transaction { BookUserMetadataTable.write(BookMetadataRow(book = id, description = null)) }
        assertEquals("From the tags", bookRepository.raw(id, libId).description)
    }

    @Test
    fun `preferEmbeddedMetadata decides between tags and agent but never beats the user`() {
        val preferring = newLibrary("prefer", folders = listOf("/media/prefer"), preferEmbeddedMetadata = true)

        val agentWins = seededBook(libId)
        val fileWins = seededBook(preferring)

        assertEquals("From the agent", bookRepository.raw(agentWins, libId).description)
        assertEquals("From the tags", bookRepository.raw(fileWins, preferring).description)

        transaction {
            listOf(agentWins, fileWins).forEach {
                BookUserMetadataTable.write(BookMetadataRow(book = it, description = "Mine"))
            }
        }
        assertEquals("Mine", bookRepository.raw(agentWins, libId).description)
        assertEquals("Mine", bookRepository.raw(fileWins, preferring).description)
    }

    private fun seededBook(libraryId: UUID): UUID {
        val id = newBook("A Book", libraryId)
        transaction {
            BookFileMetadataTable.write(BookFileMetadataTable.layer(id).copy(description = "From the tags"))
            BookAgentMetadataTable.write(BookMetadataRow(book = id, description = "From the agent"))
        }
        return id
    }

    @Test
    fun `an author list the user chose survives a rescan`() {
        trackManager.insert(scan(authors = listOf("Tagged Author")), libId)
        val id = bookId()
        assertEquals(listOf("Tagged Author"), authorNames(id), "sanity: the tags name an author")
        val chosen = newAuthor("Chosen Author", libId)

        bookRepository.modify(userId, id, libId, bookUpdate(authors = listOf(chosen)))
        trackManager.insert(scan(authors = listOf("Tagged Author")), libId)

        assertEquals(listOf("Chosen Author"), authorNames(id), "the tags must not add their author back")
    }

    @Test
    fun `an agent author list wins over the tags`() {
        trackManager.insert(scan(authors = listOf("Tagged Author")), libId)
        val id = bookId()
        val agentAuthor = authorRepository.getOrCreate("Agent Author", libId).id

        transaction {
            replaceBookAuthors(id, MetadataLayer.AGENT, listOf(agentAuthor))
            BookAgentMetadataTable.write(BookAgentMetadataTable.layer(id).copy(authorsSet = true))
        }

        assertEquals(listOf("Agent Author"), authorNames(id))
    }

    // The point of keeping the layers apart is that the file layer stays a faithful record of the tags. If a
    // rescan stopped writing it once a user override existed, clearing the override later would surface
    // whatever the tags said before the edit rather than what they say now.
    @Test
    fun `a rescan keeps updating the file layer underneath an override`() {
        trackManager.insert(scan(description = "From the tags"), libId)
        val id = bookId()
        bookRepository.modify(userId, id, libId, bookUpdate(description = "Mine"))

        trackManager.insert(scan(description = "Retagged"), libId)

        assertEquals("Mine", bookRepository.raw(id, libId).description, "the override is what is displayed")
        assertEquals(
            "Retagged",
            transaction { BookFileMetadataTable.layer(id).description },
            "the file layer must have followed the tags anyway",
        )

        transaction { BookUserMetadataTable.write(BookMetadataRow(book = id, description = null)) }
        assertEquals(
            "Retagged",
            bookRepository.raw(id, libId).description,
            "dropping the override must reveal the current tags, not the ones from before the edit",
        )
    }

    @Test
    fun `the file layer takes on an author the tags added underneath an override`() {
        trackManager.insert(scan(authors = listOf("Tagged Author")), libId)
        val id = bookId()
        bookRepository.modify(userId, id, libId, bookUpdate(authors = listOf(newAuthor("Chosen Author", libId))))

        trackManager.insert(scan(authors = listOf("Tagged Author", "Second Author")), libId)

        assertEquals(listOf("Chosen Author"), authorNames(id), "the user's author is what is displayed")
        assertEquals(
            listOf("Second Author", "Tagged Author"),
            fileLayerAuthorNames(id).sorted(),
            "the file layer must have taken the author the tags added",
        )
    }

    @Test
    fun `the file layer is cleared of a dropped series underneath an override`() {
        trackManager.insert(scan(series = "Tagged Series"), libId)
        val id = bookId()
        assertEquals(1L, fileLayerSeriesCount(id), "sanity: the tags linked a series")
        bookRepository.modify(userId, id, libId, bookUpdate(series = listOf(newSeries("Chosen Series", libId))))

        trackManager.insert(scan(series = null), libId)

        assertEquals(
            listOf("Chosen Series"),
            bookRepository.get(userId, id, libId).series.map { it.title },
            "the user's series is what is displayed",
        )
        assertEquals(0L, fileLayerSeriesCount(id), "the tags no longer name a series, so nor may the file layer")
    }

    private fun fileLayerAuthorNames(bookId: UUID): List<String> =
        transaction {
            AuthorBookTable
                .join(AuthorMetadataView, JoinType.INNER, AuthorBookTable.authors, AuthorMetadataView.id)
                .select(AuthorMetadataView.name)
                .where { (AuthorBookTable.book eq bookId) and (AuthorBookTable.addedBy eq MetadataLayer.FILE) }
                .map { it[AuthorMetadataView.name] }
        }

    private fun fileLayerSeriesCount(bookId: UUID): Long =
        transaction {
            SeriesBookTable
                .select(SeriesBookTable.series)
                .where { (SeriesBookTable.book eq bookId) and (SeriesBookTable.addedBy eq MetadataLayer.FILE) }
                .count()
        }

    @Test
    fun `a book whose tags name a different author is imported as a different book`() {
        trackManager.insert(scan(authors = listOf("Tagged Author")), libId)
        val original = bookId()

        trackManager.insert(scan(authors = listOf("Other Author")), libId)

        assertEquals(2L, bookCount(), "a book credited to someone else is not the same book")
        assertEquals(0L, trackCount(original), "the file moved to the new book, leaving the old one empty")
    }

    @Test
    fun `the book a retagged file leaves behind is cleaned up with its edits`() {
        trackManager.insert(scan(authors = listOf("Tagged Author")), libId)
        val original = bookId()
        bookRepository.modify(userId, original, libId, bookUpdate(description = "Mine"))

        trackManager.insert(scan(authors = listOf("Other Author")), libId)
        val cleanup = getKoin().get<LibraryCleanup>()
        cleanup.removeOrphans(libId)
        // The first cleanup only gives the orphan a deadline; the second one deletes it once it passed
        transaction {
            BooksTable.update({ BooksTable.deferDeletionUntil.isNotNull() }) {
                it[deferDeletionUntil] = Instant.now().minus(DEFER_DELETION_GRACE).minusSeconds(60)
            }
        }
        cleanup.removeOrphans(libId)

        assertEquals(1L, bookCount(), "nothing on disk backs the old book any more")
        assertFailsWith<ErrorResponse>("its user layer goes with it") { bookRepository.raw(original, libId) }
    }

    @Test
    fun `a series stays one series across the authors of its books`() {
        trackManager.insert(scan(authors = listOf("First Author"), series = "Shared", book = "Book One"), libId)
        trackManager.insert(scan(authors = listOf("Second Author"), series = "Shared", book = "Book Two"), libId)

        assertEquals(1L, transaction { SeriesTable.selectAll().count() }, "one series, two authors")
        assertEquals(
            listOf("First Author", "Second Author"),
            getKoin().get<SeriesRepository>().let { repo ->
                val id = transaction { repo.findByTaggedName("Shared", libId)!!.id }
                repo
                    .get(userId, id, libId)
                    .authors
                    .map { it.name }
                    .sorted()
            },
            "and it is credited to both of them",
        )
    }

    private fun bookCount() = transaction { BooksTable.selectAll().count() }

    private fun trackCount(bookId: UUID) =
        transaction { TracksTable.selectAll().where { TracksTable.book eq bookId }.count() }

    @Test
    fun `a renamed book keeps importing into the same row`() {
        trackManager.insert(scan(), libId)
        val id = bookId()

        bookRepository.modify(userId, id, libId, bookUpdate(title = "My Better Title"))
        trackManager.insert(scan(), libId)

        assertEquals("My Better Title", bookRepository.raw(id, libId).title)
        assertEquals(id, bookId(), "the tags still name the old title, which is what the file layer holds")
        assertEquals(1, bookRepository.getAll(userId, libId, SortOrder.ASC).size)
    }

    @Test
    fun `an author the user attached by hand survives orphan cleanup`() {
        trackManager.insert(scan(), libId)
        val id = bookId()
        val extra = newAuthor("Hand Picked", libId)

        bookRepository.modify(userId, id, libId, bookUpdate(authors = listOf(extra)))
        getKoin().get<LibraryCleanup>().removeOrphans(libId)

        assertEquals(listOf("Hand Picked"), authorNames(id))
    }

    @Test
    fun `every layer column is nullable and projected by the view`() {
        val flags = setOf("authorsSet", "seriesSet")
        val projected = BookMetadataView.columns.map { it.name }.toSet()

        listOf(BookFileMetadataTable, BookAgentMetadataTable, BookUserMetadataTable).forEach { layer: BookMetadata ->
            layer.columns.filterNot { it.name == "book" || it.name in flags }.forEach { column ->
                assertTrue(column.columnType.nullable, "${layer.tableName}.${column.name} must be nullable")
                assertTrue(
                    column.name in projected,
                    "${layer.tableName}.${column.name} has no column in the BookMetadata view",
                )
            }
        }
    }

    private fun authorNames(bookId: UUID) = bookRepository.get(userId, bookId, libId).authors.map { it.name }

    private fun bookUpdate(
        title: String? = null,
        description: String? = null,
        authors: List<UUID>? = null,
        series: List<UUID>? = null,
    ) = BookUpdate(
        title = title,
        authors = authors,
        series = series,
        provider = null,
        providerID = null,
        providerRating = null,
        releaseDate = null,
        publisher = null,
        language = null,
        description = description,
        narrators = null,
        genres = null,
        isbn = null,
        cover = null,
    )
}
