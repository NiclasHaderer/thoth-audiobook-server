package io.thoth.server.file

import io.thoth.models.FileScanner
import io.thoth.models.NamedMetadataAgent
import io.thoth.server.ThothTest
import io.thoth.server.database.tables.AuthorBookTable
import io.thoth.server.database.tables.AuthorEntity
import io.thoth.server.database.tables.BookEntity
import io.thoth.server.database.tables.GenreEntity
import io.thoth.server.database.tables.LibraryEntity
import io.thoth.server.database.tables.SeriesAuthorTable
import io.thoth.server.database.tables.SeriesBookTable
import io.thoth.server.database.tables.SeriesEntity
import io.thoth.server.database.tables.TrackEntity
import io.thoth.server.file.analyzer.AudioFileAnalysisResultImpl
import io.thoth.server.file.scanner.LibraryRoots
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.mp.KoinPlatform.getKoin
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.absolutePathString
import kotlin.io.path.isDirectory
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TrackManagerTest : ThothTest() {
    private val trackManager by lazy { getKoin().get<TrackManager>() }
    private val roots by lazy { getKoin().get<LibraryRoots>() }
    private var libId: UUID = UUID.randomUUID()

    private fun addPath(path: Path) {
        val library = roots.of(libId)!!
        trackManager.insert(trackManager.analyze(path, library)!!, libId)
    }

    private val testResources: Path =
        generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .map { it.resolve("test-resources") }
            .first { it.isDirectory() }

    // Dan Brown / Robert Langdon / 01 - Angels and Demons -> author + series + book
    private val bookWithSeries: Path =
        testResources
            .resolve("Dan Brown")
            .resolve("Robert Langdon")
            .toFile()
            .walkTopDown()
            .first { it.isFile && it.extension == "mp3" }
            .toPath()

    @BeforeTest
    fun createLibrary() {
        libId =
            transaction {
                LibraryEntity
                    .new {
                        name = "lib"
                        folders = listOf(testResources.absolutePathString())
                        metadataAgents = listOf(NamedMetadataAgent("audible"))
                        fileScanners = listOf(FileScanner("AudioTagScanner"), FileScanner("AudioFolderScanner"))
                        language = "en"
                    }.id
                    .value
            }
    }

    @Test
    fun `adding a file links its book, author and series`() {
        addPath(bookWithSeries)

        transaction {
            assertEquals(1L, TrackEntity.all().count(), "the track must be imported")
            assertNotNull(BookEntity.all().firstOrNull(), "the book must be created")
            assertNotNull(AuthorEntity.all().firstOrNull(), "the author must be created")
            assertNotNull(SeriesEntity.all().firstOrNull(), "the series must be created")
            assertEquals(1L, AuthorBookTable.selectAll().count(), "the book must be linked to its author")
            assertEquals(1L, SeriesBookTable.selectAll().count(), "the book must be linked to its series")
            assertEquals(1L, SeriesAuthorTable.selectAll().count(), "the series must be linked to its author")
        }
    }

    @Test
    fun `genres from the tags land on the book and on its series`() {
        trackManager.insert(taggedWith("Fantasy", "Sci-Fi"), libId)

        transaction {
            val book = BookEntity.all().first()
            assertEquals(setOf("Fantasy", "Sci-Fi"), book.genres.map { it.name }.toSet())
            assertEquals(setOf("Fantasy", "Sci-Fi"), SeriesEntity.all().first().genres.map { it.name }.toSet())
        }
    }

    @Test
    fun `a genre spelled differently by a second file is not duplicated`() {
        trackManager.insert(taggedWith("Fantasy"), libId)
        trackManager.insert(taggedWith("fantasy", "FANTASY"), libId)

        transaction {
            assertEquals(1L, GenreEntity.all().count(), "the genre must be reused across spellings")
        }
    }

    // The test files carry no tags at all, so a scan result is handed to the importer directly
    private fun taggedWith(vararg genres: String) =
        AudioFileAnalysisResultImpl(
            title = "Angels and Demons",
            authors = listOf("Dan Brown"),
            book = "Angels and Demons",
            series = "Robert Langdon",
            duration = 60,
            path = bookWithSeries.absolutePathString(),
            lastModified = 0,
            genres = genres.toList(),
        )

    @Test
    fun `adding a second file of the same book reuses the author and series`() {
        addPath(bookWithSeries)
        val second =
            bookWithSeries.parent
                .toFile()
                .walkTopDown()
                .first { it.isFile && it.extension == "mp3" && it.toPath() != bookWithSeries }
                .toPath()

        addPath(second)

        transaction {
            assertEquals(2L, TrackEntity.all().count())
            assertEquals(1L, AuthorEntity.all().count(), "the author must not be duplicated")
            assertEquals(1L, SeriesEntity.all().count(), "the series must not be duplicated")
            assertEquals(1L, SeriesAuthorTable.selectAll().count())
        }
    }
}
