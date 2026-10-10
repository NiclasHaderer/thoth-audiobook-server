package io.thoth.server.database

import io.thoth.metadata.FakeMetadataAgent
import io.thoth.metadata.MetadataAgents
import io.thoth.metadata.searchHit
import io.thoth.metadata.testBook
import io.thoth.models.AuthorUpdate
import io.thoth.models.BookUpdate
import io.thoth.models.NamedMetadataAgent
import io.thoth.models.SeriesUpdate
import io.thoth.openapi.common.Patch
import io.thoth.openapi.common.orAbsent
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.ThothTest
import io.thoth.server.database.tables.BookField
import io.thoth.server.database.tables.BookTable
import io.thoth.server.database.tables.SeriesBookTable
import io.thoth.server.database.tables.SeriesTable
import io.thoth.server.database.tables.TrackTable
import io.thoth.server.file.TrackManager
import io.thoth.server.file.analyzer.AudioFileAnalysisResultImpl
import io.thoth.server.file.scanner.LibraryCleanup
import io.thoth.server.newAuthor
import io.thoth.server.newLibrary
import io.thoth.server.newSeries
import io.thoth.server.newUser
import io.thoth.server.repositories.AuthorRepository
import io.thoth.server.repositories.BookRepository
import io.thoth.server.repositories.DEFER_DELETION_GRACE
import io.thoth.server.repositories.SeriesRepository
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.dsl.module
import org.koin.mp.KoinPlatform.getKoin
import java.time.Instant
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MetadataWriteTest : ThothTest() {
    private val bookRepository by lazy { getKoin().get<BookRepository>() }
    private val authorRepository by lazy { getKoin().get<AuthorRepository>() }
    private val trackManager by lazy { getKoin().get<TrackManager>() }

    private var libId: UUID = UUID.randomUUID()
    private var userId: UUID = UUID.randomUUID()

    @BeforeTest
    fun createLibrary() {
        libId = newLibrary("lib", folders = listOf("/media/books"), metadataAgents = listOf(NamedMetadataAgent("fake")))
        userId = newUser("test-user", admin = true)
    }

    private fun scan(
        authors: List<String> = listOf("An Author"),
        description: String? = null,
        narrators: List<String> = emptyList(),
        series: String? = null,
        seriesIndex: Float? = null,
        book: String = "A Book",
        path: String = "/media/books/A Book/01.mp3",
    ) = AudioFileAnalysisResultImpl(
        title = "Chapter 1",
        authors = authors,
        book = book,
        series = series,
        seriesIndex = seriesIndex,
        durationMs = 60_000,
        path = path,
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
        assertEquals(setOf(BookField.DESCRIPTION), book.locked)
    }

    @Test
    fun `unlocking keeps the value until the next scan writes the field`() {
        trackManager.insert(scan(description = "From the tags"), libId)
        val id = bookId()
        bookRepository.modify(userId, id, libId, bookUpdate(description = "Mine"))

        bookRepository.modify(userId, id, libId, BookUpdate(unlock = Patch.Set(listOf(BookField.DESCRIPTION))))
        assertEquals("Mine", bookRepository.raw(id, libId).description, "unlocking alone changes no value")
        assertEquals(emptySet(), bookRepository.raw(id, libId).locked)

        trackManager.insert(scan(description = "Retagged"), libId)
        assertEquals("Retagged", bookRepository.raw(id, libId).description)
    }

    @Test
    fun `the files only fill the gaps of a matched book unless the library prefers them`() {
        val preferring = newLibrary("prefer", folders = listOf("/media/prefer"), preferEmbeddedMetadata = true)
        val agentWins = matchedBook(libId, "/media/books/A Book/01.mp3")
        val fileWins = matchedBook(preferring, "/media/prefer/A Book/01.mp3")

        trackManager.insert(
            scan(description = "Retagged", narrators = listOf("Tagged"), path = "/media/books/A Book/01.mp3"),
            libId,
        )
        trackManager.insert(
            scan(description = "Retagged", narrators = listOf("Tagged"), path = "/media/prefer/A Book/01.mp3"),
            preferring,
        )

        assertEquals("From the agent", bookRepository.raw(agentWins, libId).description)
        assertEquals(listOf("Tagged"), bookRepository.raw(agentWins, libId).narrators, "an empty field is a gap")
        assertEquals("Retagged", bookRepository.raw(fileWins, preferring).description)
    }

    // What an agent match leaves behind, without going through one
    private fun matchedBook(
        libraryId: UUID,
        path: String,
    ): UUID {
        trackManager.insert(scan(description = "From the tags", path = path), libraryId)
        val id = transaction { bookRepository.findByTaggedName("A Book", emptyList(), libraryId)!!.id }
        transaction {
            BookTable.update({ BookTable.id eq id }) {
                it[provider] = "fake"
                it[providerId] = "a-book"
                it[description] = "From the agent"
            }
        }
        return id
    }

    @Test
    fun `a match neither touches a locked field nor overwrites the files the library prefers`() {
        val agent =
            FakeMetadataAgent(
                hits = listOf(searchHit("A Book", authors = listOf("An Author"))),
                resolveBook = {
                    testBook(
                        it,
                    ).copy(title = "Agent Title", description = "From the agent", isbn = "123")
                },
            )
        getKoin().loadModules(listOf(module { single { MetadataAgents(listOf(agent)) } }), allowOverride = true)
        val preferring =
            newLibrary(
                "prefer",
                folders = listOf("/media/prefer"),
                preferEmbeddedMetadata = true,
                metadataAgents = listOf(NamedMetadataAgent("fake")),
            )
        trackManager.insert(scan(description = "From the tags"), libId)
        trackManager.insert(scan(description = "From the tags", path = "/media/prefer/A Book/01.mp3"), preferring)
        val locked = bookId()
        val fileWins = transaction { bookRepository.findByTaggedName("A Book", emptyList(), preferring)!!.id }
        bookRepository.modify(userId, locked, libId, bookUpdate(title = "Mine"))

        bookRepository.autoMatch(userId, locked, libId)
        bookRepository.autoMatch(userId, fileWins, preferring)

        val matched = bookRepository.raw(locked, libId)
        assertEquals("Mine", matched.title, "a locked field is never matched over")
        assertEquals("From the agent", matched.description)
        assertEquals("fake", matched.provider)
        val preferred = bookRepository.raw(fileWins, preferring)
        assertEquals("From the tags", preferred.description, "the files are preferred, the agent only fills gaps")
        assertEquals("123", preferred.isbn)
        assertEquals("fake", preferred.provider, "the match itself is always recorded")
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
    fun `a file whose album tag changed moves to another book`() {
        trackManager.insert(scan(), libId)
        val original = bookId()

        trackManager.insert(scan(book = "Another Book"), libId)

        assertEquals(2L, bookCount())
        assertEquals(0L, trackCount(original), "the file moved to the new book, leaving the old one empty")
    }

    @Test
    fun `a new file of a book renamed by the user joins that book`() {
        trackManager.insert(scan(), libId)
        val id = bookId()
        bookRepository.modify(userId, id, libId, bookUpdate(title = "My Better Title"))

        trackManager.insert(scan(path = "/media/books/A Book/02.mp3"), libId)

        assertEquals("My Better Title", bookRepository.raw(id, libId).title)
        assertEquals(2L, trackCount(id), "the files still name the old title, which the book remembers")
        assertEquals(1, bookRepository.getAll(userId, libId, SortOrder.ASC).size)
    }

    @Test
    fun `a file tagged with the new title does not split off the files tagged with the old one`() {
        trackManager.insert(scan(), libId)
        val id = bookId()
        bookRepository.modify(userId, id, libId, bookUpdate(title = "My Better Title"))
        trackManager.insert(scan(book = "My Better Title", path = "/media/books/A Book/02.mp3"), libId)

        trackManager.insert(scan(description = "Retagged"), libId)

        assertEquals(1L, bookCount(), "both files belong to the one book")
        assertEquals(2L, trackCount(id))
        assertEquals("A Book", bookRepository.raw(id, libId).taggedName)
    }

    @Test
    fun `an author and a series renamed by the user are still found by their tagged names`() {
        trackManager.insert(scan(authors = listOf("Tagged Author"), series = "Tagged Series"), libId)
        val book = bookRepository.get(userId, bookId(), libId)
        val authorId = book.authors.single().id
        val seriesId = book.series.single().id
        val seriesRepository = getKoin().get<SeriesRepository>()
        authorRepository.modify(userId, authorId, libId, AuthorUpdate(name = Patch.Set("Renamed")))
        seriesRepository.modify(userId, seriesId, libId, SeriesUpdate(title = Patch.Set("Renamed")))

        assertEquals(authorId, authorRepository.findByTaggedName("Tagged Author", libId)?.id)
        assertEquals(seriesId, seriesRepository.findByTaggedName("Tagged Series", libId)?.id)
    }

    @Test
    fun `the book a retagged file leaves behind is cleaned up with its edits`() {
        trackManager.insert(scan(), libId)
        val original = bookId()
        bookRepository.modify(userId, original, libId, bookUpdate(description = "Mine"))

        trackManager.insert(scan(book = "Another Book"), libId)
        val cleanup = getKoin().get<LibraryCleanup>()
        cleanup.removeOrphans(libId)
        // The first cleanup only gives the orphan a deadline; the second one deletes it once it passed
        transaction {
            BookTable.update({ BookTable.deferDeletionUntil.isNotNull() }) {
                it[deferDeletionUntil] = Instant.now().minus(DEFER_DELETION_GRACE).minusSeconds(60)
            }
        }
        cleanup.removeOrphans(libId)

        assertEquals(1L, bookCount(), "nothing on disk backs the old book any more")
        assertFailsWith<ErrorResponse> { bookRepository.raw(original, libId) }
    }

    @Test
    fun `a series stays one series across the authors of its books`() {
        trackManager.insert(
            scan(
                authors = listOf("First Author"),
                series = "Shared",
                book = "Book One",
                path = "/media/books/1/01.mp3",
            ),
            libId,
        )
        trackManager.insert(
            scan(
                authors = listOf("Second Author"),
                series = "Shared",
                book = "Book Two",
                path = "/media/books/2/01.mp3",
            ),
            libId,
        )

        assertEquals(1L, transaction { SeriesTable.selectAll().count() }, "one series, two authors")
        val repo = getKoin().get<SeriesRepository>()
        val id = transaction { repo.findByTaggedName("Shared", libId)!!.id }
        assertEquals(
            listOf("First Author", "Second Author"),
            repo
                .get(userId, id, libId)
                .authors
                .map { it.name }
                .sorted(),
        )
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
    fun `a blanked field stays blank through a rescan until it is set`() {
        trackManager.insert(scan(description = "From the tags"), libId)
        val id = bookId()

        bookRepository.modify(userId, id, libId, BookUpdate(description = Patch.Set(null)))
        trackManager.insert(scan(description = "Retagged"), libId)
        assertNull(bookRepository.raw(id, libId).description, "a rescan must not fill a blanked field")

        bookRepository.modify(userId, id, libId, bookUpdate(title = "Other field"))
        assertNull(bookRepository.raw(id, libId).description, "editing another field must keep the blank")

        bookRepository.modify(userId, id, libId, bookUpdate(description = "Mine"))
        assertEquals("Mine", bookRepository.raw(id, libId).description)
    }

    @Test
    fun `resubmitting the current values locks nothing`() {
        trackManager.insert(scan(description = "From the tags"), libId)
        val id = bookId()
        val authors = bookRepository.get(userId, id, libId).authors.map { it.id }

        bookRepository.modify(
            userId,
            id,
            libId,
            bookUpdate(title = "A Book", description = "From the tags", authors = authors),
        )
        trackManager.insert(scan(description = "Retagged"), libId)

        assertEquals(emptySet(), bookRepository.raw(id, libId).locked)
        assertEquals("Retagged", bookRepository.raw(id, libId).description)
    }

    // TODO a rescan no longer finds a book whose authors a user changed: the tags still name the old authors, so
    //  the file moves to a new book and this one is left to the cleanup. Enable once a known file stays with its book.
    @Ignore
    @Test
    fun `unlocking hand picked authors lets the next scan bring back the tagged ones`() {
        trackManager.insert(scan(), libId)
        val id = bookId()
        bookRepository.modify(userId, id, libId, bookUpdate(authors = listOf(newAuthor("Hand Picked", libId))))

        bookRepository.modify(userId, id, libId, BookUpdate(unlock = Patch.Set(listOf(BookField.AUTHORS))))
        assertEquals(listOf("Hand Picked"), authorNames(id))

        trackManager.insert(scan(), libId)
        assertEquals(listOf("An Author"), authorNames(id))
    }

    @Test
    fun `a blanked series list stays empty through a rescan`() {
        trackManager.insert(scan(series = "Tagged Series"), libId)
        val id = bookId()
        assertEquals(1, bookRepository.get(userId, id, libId).series.size, "sanity: the tags name a series")

        bookRepository.modify(userId, id, libId, BookUpdate(series = Patch.Set(null)))
        trackManager.insert(scan(series = "Tagged Series"), libId)

        assertEquals(emptyList(), bookRepository.get(userId, id, libId).series.map { it.title })
    }

    @Test
    fun `a user moving a book between series keeps the index the tags gave it`() {
        trackManager.insert(scan(series = "Tagged Series", seriesIndex = 3f), libId)
        val id = bookId()
        val series = bookRepository
            .get(userId, id, libId)
            .series
            .single()
            .id
        val other = newSeries("Other Series", libId)

        bookRepository.modify(userId, id, libId, bookUpdate(series = listOf(series, other)))

        val index =
            transaction {
                SeriesBookTable
                    .selectAll()
                    .where { SeriesBookTable.series eq series }
                    .single()[SeriesBookTable.seriesIndex]
            }
        assertEquals(3f, index)
    }

    private fun bookCount() = transaction { BookTable.selectAll().count() }

    private fun trackCount(bookId: UUID) =
        transaction { TrackTable.selectAll().where { TrackTable.book eq bookId }.count() }

    private fun authorNames(bookId: UUID) = bookRepository.get(userId, bookId, libId).authors.map { it.name }

    private fun bookUpdate(
        title: String? = null,
        description: String? = null,
        authors: List<UUID>? = null,
        series: List<UUID>? = null,
    ) = BookUpdate(
        title = title.orAbsent(),
        authors = authors.orAbsent(),
        series = series.orAbsent(),
        description = description.orAbsent(),
    )
}
