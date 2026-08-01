package io.thoth.server.repositories

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.http.HttpStatusCode
import io.thoth.models.FileScanner
import io.thoth.models.NamedMetadataAgent
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.database.tables.AuthorBookTable
import io.thoth.server.database.tables.AuthorEntity
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.GenreBookTable
import io.thoth.server.database.tables.GenreSeriesTable
import io.thoth.server.database.tables.GenresTable
import io.thoth.server.database.tables.ImageTable
import io.thoth.server.database.tables.LibrariesTable
import io.thoth.server.database.tables.LibraryEntity
import io.thoth.server.database.tables.LibraryUserTable
import io.thoth.server.database.tables.SeriesAuthorTable
import io.thoth.server.database.tables.SeriesBookTable
import io.thoth.server.database.tables.SeriesTable
import io.thoth.server.database.tables.TracksTable
import io.thoth.server.database.tables.UsersTable
import io.thoth.server.di.serialization.JacksonSerialization
import io.thoth.server.di.serialization.Serialization
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.File
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RepositoryTest {
    private val libraryRepository = LibraryRepositoryImpl()
    private val authorRepository = AuthorServiceImpl()
    private val bookRepository = BookRepositoryImpl()
    private val seriesRepository = SeriesRepositoryImpl()

    private lateinit var dbFile: File
    private var libId: UUID = UUID.randomUUID()

    @BeforeTest
    fun setup() {
        // LibrariesTable's json columns resolve their serializer through Koin while the table object initializes,
        // so Koin has to be up before any table is touched.
        startKoin {
            modules(
                module {
                    single { JacksonSerialization().apply { objectMapper = jacksonObjectMapper() } }
                    single<Serialization> { get<JacksonSerialization>() }
                    single<LibraryRepository> { libraryRepository }
                    single<AuthorRepository> { authorRepository }
                    single<BookRepository> { bookRepository }
                    single<SeriesRepository> { seriesRepository }
                },
            )
        }
        dbFile = File.createTempFile("thoth-repository-test", ".db")
        Database.connect("jdbc:sqlite:${dbFile.absolutePath}", "org.sqlite.JDBC")
        transaction {
            SchemaUtils.create(
                LibrariesTable,
                ImageTable,
                AuthorTable,
                BooksTable,
                SeriesTable,
                GenresTable,
                TracksTable,
                UsersTable,
                AuthorBookTable,
                GenreBookTable,
                GenreSeriesTable,
                SeriesBookTable,
                SeriesAuthorTable,
                LibraryUserTable,
            )
        }
        libId = newLibrary("lib", "/media/books")
    }

    @AfterTest
    fun teardown() {
        stopKoin()
        dbFile.delete()
    }

    private fun newLibrary(
        libraryName: String,
        folder: String,
    ) = transaction {
        LibraryEntity
            .new {
                name = libraryName
                folders = listOf(folder)
                metadataAgents = listOf(NamedMetadataAgent("audible"))
                fileScanners = listOf(FileScanner("AudioFolderScanner"))
                language = "en"
            }.id
            .value
    }

    private fun newAuthor(authorName: String) =
        transaction {
            AuthorEntity
                .new {
                    name = authorName
                    library = LibraryEntity[libId]
                }.id
                .value
        }

    @Test
    fun `author findByName matches ignoring case`() {
        newAuthor("Stephen King")
        assertNotNull(authorRepository.findByName("stephen king", libId))
        assertNotNull(authorRepository.findByName("STEPHEN KING", libId))
    }

    @Test
    fun `author findByName treats like wildcards as literals`() {
        newAuthor("Stephen King")
        assertNull(authorRepository.findByName("Stephen_King", libId), "'_' must not act as a wildcard")
        assertNull(authorRepository.findByName("%", libId), "'%' must not act as a wildcard")
        assertNull(authorRepository.findByName("Stephen%", libId), "trailing '%' must not act as a wildcard")
    }

    @Test
    fun `author search matches ignoring case and escapes wildcards`() {
        newAuthor("Brandon Sanderson")
        newAuthor("100% Author")
        assertEquals(listOf("Brandon Sanderson"), authorRepository.search("sanderson", libId).map { it.name })
        assertEquals(listOf("100% Author"), authorRepository.search("100%", libId).map { it.name })
        assertEquals(emptyList(), authorRepository.search("Brandon_Sanderson", libId).map { it.name })
    }

    @Test
    fun `author search is scoped to the given library`() {
        newAuthor("Brandon Sanderson")
        val otherLib = newLibrary("other", "/media/other")
        assertEquals(emptyList(), authorRepository.search("sanderson", otherLib).map { it.name })
        assertEquals(listOf("Brandon Sanderson"), authorRepository.search("sanderson").map { it.name })
    }

    @Test
    fun `author getOrCreate reuses an author that differs only in casing`() {
        val first = transaction { authorRepository.getOrCreate("Terry Pratchett", libId).id.value }
        val second = transaction { authorRepository.getOrCreate("terry pratchett", libId).id.value }
        assertEquals(first, second)
        assertEquals(1L, transaction { AuthorEntity.find { AuthorTable.library eq libId }.count() })
    }

    @Test
    fun `author position reports the index in the requested order`() {
        newAuthor("Bbb")
        val first = newAuthor("Aaa")
        newAuthor("Ccc")
        assertEquals(0L, authorRepository.position(first, libId, SortOrder.ASC))
        assertEquals(2L, authorRepository.position(first, libId, SortOrder.DESC))
    }

    @Test
    fun `author position rejects an author that is not in the library`() {
        newAuthor("Aaa")
        // Swapping id and libraryId (as the route used to) has to fail loudly instead of yielding -1.
        val error = assertFailsWith<ErrorResponse> { authorRepository.position(libId, libId, SortOrder.ASC) }
        assertEquals(HttpStatusCode.NotFound, error.status)
    }

    @Test
    fun `book findByName finds a book that has no authors`() {
        val created = transaction { bookRepository.create("Orphan Book", libId, emptyList(), emptyList()).id.value }
        val found = bookRepository.findByName("Orphan Book", emptyList(), libId)
        assertNotNull(found, "a book without authors must still be findable by title")
        assertEquals(created, transaction { found.id.value })
    }

    @Test
    fun `book findByName stays scoped to the given authors`() {
        transaction {
            val wanted = authorRepository.getOrCreate("Wanted", libId)
            val other = authorRepository.getOrCreate("Other", libId)
            val book = bookRepository.create("Shared Title", libId, listOf(wanted), emptyList())
            assertEquals(book.id, bookRepository.findByName("shared title", listOf(wanted.id.value), libId)?.id)
            assertNull(bookRepository.findByName("Shared Title", listOf(other.id.value), libId))
        }
    }

    @Test
    fun `book findByName treats like wildcards as literals`() {
        val authorId =
            transaction {
                val author = authorRepository.getOrCreate("Author", libId)
                bookRepository.create("Book One", libId, listOf(author), emptyList())
                author.id.value
            }
        assertNotNull(bookRepository.findByName("book one", listOf(authorId), libId), "sanity: the book exists")
        assertNull(bookRepository.findByName("Book_One", listOf(authorId), libId))
        assertNull(bookRepository.findByName("Book%", listOf(authorId), libId))
        assertNull(bookRepository.findByName("Book_One", emptyList(), libId))
    }

    @Test
    fun `book position reports the index in the requested order`() {
        val (first, last) =
            transaction {
                val a = bookRepository.create("Aaa", libId, emptyList(), emptyList()).id.value
                val c = bookRepository.create("Ccc", libId, emptyList(), emptyList()).id.value
                bookRepository.create("Bbb", libId, emptyList(), emptyList())
                a to c
            }
        assertEquals(0L, bookRepository.position(first, libId, SortOrder.ASC))
        assertEquals(2L, bookRepository.position(first, libId, SortOrder.DESC))
        assertEquals(2L, bookRepository.position(last, libId, SortOrder.ASC))
        assertEquals(0L, bookRepository.position(last, libId, SortOrder.DESC))
    }

    @Test
    fun `book search orders results by title`() {
        transaction {
            bookRepository.create("The Zebra Mystery", libId, emptyList(), emptyList())
            bookRepository.create("The Antelope Mystery", libId, emptyList(), emptyList())
            bookRepository.create("The Mule Mystery", libId, emptyList(), emptyList())
        }
        assertEquals(
            listOf("The Antelope Mystery", "The Mule Mystery", "The Zebra Mystery"),
            bookRepository.search("mystery", libId).map { it.title },
        )
    }

    @Test
    fun `series raw works without a surrounding transaction`() {
        val id = transaction { seriesRepository.create("Mistborn", libId, emptyList()).id.value }
        assertEquals(id, seriesRepository.raw(id, libId).id.value)
    }

    @Test
    fun `series getOrCreate works without a surrounding transaction`() {
        val id = seriesRepository.getOrCreate("Stormlight", libId, emptyList()).id.value
        assertEquals(id, seriesRepository.getOrCreate("Stormlight", libId, emptyList()).id.value)
        assertEquals(1L, transaction { SeriesTable.selectAll().count() })
    }

    @Test
    fun `series getOrCreate links an author only once`() {
        val author = transaction { authorRepository.getOrCreate("Sanderson", libId) }
        seriesRepository.getOrCreate("Stormlight", libId, listOf(author))
        seriesRepository.getOrCreate("Stormlight", libId, listOf(author))
        assertEquals(1L, transaction { SeriesAuthorTable.selectAll().count() })
    }

    @Test
    fun `series position rejects a series that is not in the library`() {
        transaction { seriesRepository.create("Mistborn", libId, emptyList()) }
        val error = assertFailsWith<ErrorResponse> { seriesRepository.position(libId, libId, SortOrder.ASC) }
        assertEquals(HttpStatusCode.NotFound, error.status)
    }

    @Test
    fun `library folders overlapping an existing library are rejected in both directions`() {
        assertTrue(
            libraryRepository.overlappingFolders(null, listOf("/media/books/scifi")).first,
            "a folder inside an existing library folder overlaps",
        )
        assertTrue(
            libraryRepository.overlappingFolders(null, listOf("/media")).first,
            "a folder containing an existing library folder overlaps",
        )
        assertTrue(
            libraryRepository.overlappingFolders(null, listOf("/media/books")).first,
            "an identical folder overlaps",
        )
    }

    @Test
    fun `library folder overlap ignores unrelated folders and the library itself`() {
        assertFalse(libraryRepository.overlappingFolders(null, listOf("/other")).first)
        assertFalse(
            libraryRepository.overlappingFolders(libId, listOf("/media/books")).first,
            "a library must not overlap with itself",
        )
        assertFalse(
            libraryRepository.overlappingFolders(null, listOf("/media/booksomething")).first,
            "a sibling with a shared name prefix is not nested",
        )
    }
}
