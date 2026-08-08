package io.thoth.server.file

import io.thoth.models.FileScanner
import io.thoth.server.ThothTest
import io.thoth.server.database.tables.AuthorBookTable
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.ImageTable
import io.thoth.server.database.tables.GenreBookTable
import io.thoth.server.database.tables.GenreSeriesTable
import io.thoth.server.database.access.getOrCreateImage
import io.thoth.server.database.tables.GenresTable
import io.thoth.server.database.tables.SeriesAuthorTable
import io.thoth.server.database.tables.SeriesBookTable
import io.thoth.server.database.tables.SeriesTable
import io.thoth.server.database.tables.TracksTable
import io.thoth.server.file.analyzer.AudioFileAnalysisResultImpl
import io.thoth.server.file.scanner.LibraryRoots
import io.thoth.server.newLibrary
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
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
            newLibrary(
                "lib",
                folders = listOf(testResources.absolutePathString()),
                fileScanners = listOf(FileScanner("AudioTagScanner"), FileScanner("AudioFolderScanner")),
            )
    }

    @Test
    fun `adding a file links its book, author and series`() {
        addPath(bookWithSeries)

        transaction {
            assertEquals(1L, TracksTable.selectAll().count(), "the track must be imported")
            assertNotNull(BooksTable.selectAll().firstOrNull(), "the book must be created")
            assertNotNull(AuthorTable.selectAll().firstOrNull(), "the author must be created")
            assertNotNull(SeriesTable.selectAll().firstOrNull(), "the series must be created")
            assertEquals(1L, AuthorBookTable.selectAll().count(), "the book must be linked to its author")
            assertEquals(1L, SeriesBookTable.selectAll().count(), "the book must be linked to its series")
            assertEquals(1L, SeriesAuthorTable.selectAll().count(), "the series must be linked to its author")
        }
    }

    @Test
    fun `genres from the tags land on the book and on its series`() {
        trackManager.insert(taggedWith("Fantasy", "Sci-Fi"), libId)

        transaction {
            val bookGenres =
                (GenreBookTable innerJoin GenresTable)
                    .select(GenresTable.name)
                    .map { it[GenresTable.name] }
                    .toSet()
            val seriesGenres =
                (GenreSeriesTable innerJoin GenresTable)
                    .select(GenresTable.name)
                    .map { it[GenresTable.name] }
                    .toSet()
            assertEquals(setOf("Fantasy", "Sci-Fi"), bookGenres)
            assertEquals(setOf("Fantasy", "Sci-Fi"), seriesGenres)
        }
    }

    @Test
    fun `a genre spelled differently by a second file is not duplicated`() {
        trackManager.insert(taggedWith("Fantasy"), libId)
        trackManager.insert(taggedWith("fantasy", "FANTASY"), libId)

        transaction {
            assertEquals(1L, GenresTable.selectAll().count(), "the genre must be reused across spellings")
        }
    }

    private fun scanWithCover(
        cover: ByteArray,
        fileName: String = bookWithSeries.absolutePathString(),
    ) = AudioFileAnalysisResultImpl(
        title = "Angels and Demons",
        authors = listOf("Dan Brown"),
        book = "Angels and Demons",
        series = "Robert Langdon",
        duration = 60,
        path = fileName,
        lastModified = 0,
        cover = cover,
    )

    private fun coverOf(bookId: UUID) =
        transaction {
            BooksTable.select(BooksTable.coverID).where { BooksTable.id eq bookId }.single()[BooksTable.coverID]?.value
        }

    @Test
    fun `a rescan does not overwrite a cover that was edited`() {
        trackManager.insert(scanWithCover(byteArrayOf(1, 2, 3)), libId)
        val bookId = transaction { BooksTable.select(BooksTable.id).single()[BooksTable.id].value }

        // Stand-in for a metadata match or a hand edit pointing the book at different art
        val edited = transaction { getOrCreateImage(byteArrayOf(9, 9, 9), null)!! }
        transaction { BooksTable.update({ BooksTable.id eq bookId }) { it[coverID] = edited } }

        // The file changed on disk and is re-imported, still carrying its original embedded art
        trackManager.insert(scanWithCover(byteArrayOf(1, 2, 3)), libId)

        assertEquals(edited, coverOf(bookId), "the file's embedded art must not replace an edited cover")
    }

    @Test
    fun `tracks of one book with differing embedded art store one image`() {
        trackManager.insert(scanWithCover(byteArrayOf(1, 2, 3), "${bookWithSeries.absolutePathString()}.1"), libId)
        trackManager.insert(scanWithCover(byteArrayOf(4, 5, 6), "${bookWithSeries.absolutePathString()}.2"), libId)

        assertEquals(
            1L,
            transaction { ImageTable.selectAll().count() },
            "the first cover wins, so a second track's art must not add a row",
        )
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
            assertEquals(2L, TracksTable.selectAll().count())
            assertEquals(1L, AuthorTable.selectAll().count(), "the author must not be duplicated")
            assertEquals(1L, SeriesTable.selectAll().count(), "the series must not be duplicated")
            assertEquals(1L, SeriesAuthorTable.selectAll().count())
        }
    }
}
