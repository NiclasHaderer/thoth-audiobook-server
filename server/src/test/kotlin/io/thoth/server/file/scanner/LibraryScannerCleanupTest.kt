package io.thoth.server.file.scanner

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.thoth.models.FileScanner
import io.thoth.models.NamedMetadataAgent
import io.thoth.server.database.tables.AuthorBookTable
import io.thoth.server.database.tables.AuthorEntity
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.BookEntity
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
import io.thoth.server.database.tables.SeriesEntity
import io.thoth.server.database.tables.SeriesTable
import io.thoth.server.database.tables.TrackEntity
import io.thoth.server.database.tables.TracksTable
import io.thoth.server.database.tables.UsersTable
import io.thoth.server.di.serialization.JacksonSerialization
import io.thoth.server.di.serialization.Serialization
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.SizedCollection
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

class LibraryScannerCleanupTest {
    private val scanner = LibraryScannerImpl()

    private lateinit var dbFile: File

    @BeforeTest
    fun setup() {
        // LibrariesTable's json columns resolve their serializer through Koin while the table object initializes,
        // so Koin has to be up before any table is touched.
        startKoin {
            modules(
                module {
                    single { JacksonSerialization().apply { objectMapper = jacksonObjectMapper() } }
                    single<Serialization> { get<JacksonSerialization>() }
                },
            )
        }
        dbFile = File.createTempFile("thoth-scanner-cleanup-test", ".db")
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
    }

    @AfterTest
    fun teardown() {
        stopKoin()
        dbFile.delete()
    }

    private fun newLibrary(libraryName: String): UUID =
        transaction {
            LibraryEntity
                .new {
                    name = libraryName
                    folders = listOf("/media/$libraryName")
                    metadataAgents = listOf(NamedMetadataAgent("audible"))
                    fileScanners = listOf(FileScanner("AudioFolderScanner"))
                    language = "en"
                }.id
                .value
        }

    private fun newBookWithTrack(
        libraryId: UUID,
        prefix: String,
        trackScanIndex: ULong,
    ) = transaction {
        val lib = LibraryEntity[libraryId]
        val bookAuthor = AuthorEntity.new { name = "$prefix Author"; library = lib }
        val bookSeries = SeriesEntity.new { title = "$prefix Series"; library = lib }
        val newBook =
            BookEntity.new {
                title = "$prefix Book"
                library = lib
                authors = SizedCollection(listOf(bookAuthor))
                series = SizedCollection(listOf(bookSeries))
            }
        TrackEntity.new {
            title = "$prefix Track"
            path = "/media/$prefix/track.mp3"
            duration = 1
            accessTime = 0
            scanIndex = trackScanIndex
            book = newBook
            library = lib
        }
    }

    private fun counts(libraryId: UUID) =
        transaction {
            listOf(
                TrackEntity.find { TracksTable.library eq libraryId }.count(),
                BookEntity.find { BooksTable.library eq libraryId }.count(),
                AuthorEntity.find { AuthorTable.library eq libraryId }.count(),
                SeriesEntity.find { SeriesTable.library eq libraryId }.count(),
            )
        }

    @Test
    fun `cleanup leaves other libraries untouched`() {
        val scanned = newLibrary("scanned")
        val other = newLibrary("other")
        newBookWithTrack(scanned, "scanned", trackScanIndex = 1uL)
        newBookWithTrack(other, "other", trackScanIndex = 1uL)
        transaction { LibraryEntity[scanned].scanIndex = 2uL }

        scanner.cleanupLibrary(transaction { LibraryEntity[scanned] })

        assertEquals(
            listOf(1L, 1L, 1L, 1L),
            counts(other),
            "rescanning one library must not delete another library's track, book, author or series",
        )
    }

    @Test
    fun `cleanup removes content of the scanned library that is no longer on disk`() {
        val scanned = newLibrary("scanned")
        newBookWithTrack(scanned, "scanned", trackScanIndex = 1uL)
        transaction { LibraryEntity[scanned].scanIndex = 2uL }

        scanner.cleanupLibrary(transaction { LibraryEntity[scanned] })

        assertEquals(
            listOf(0L, 0L, 0L, 0L),
            counts(scanned),
            "a track that was not touched by the scan must be removed together with its orphaned relations",
        )
    }

    @Test
    fun `cleanup keeps content that the scan touched`() {
        val scanned = newLibrary("scanned")
        newBookWithTrack(scanned, "scanned", trackScanIndex = 2uL)
        transaction { LibraryEntity[scanned].scanIndex = 2uL }

        scanner.cleanupLibrary(transaction { LibraryEntity[scanned] })

        assertEquals(listOf(1L, 1L, 1L, 1L), counts(scanned), "a touched track and its relations must survive")
    }
}
