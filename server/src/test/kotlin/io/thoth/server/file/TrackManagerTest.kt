package io.thoth.server.file

import io.ktor.http.Headers
import io.ktor.server.testing.ApplicationTestBuilder
import io.thoth.client.gen.models.BookDetailed
import io.thoth.client.gen.models.LibraryPermissionLevel
import io.thoth.client.gen.models.SeriesDetailed
import io.thoth.models.FileScanner
import io.thoth.server.ThothTest
import io.thoth.server.api
import io.thoth.server.bearer
import io.thoth.server.database.access.getOrCreateImage
import io.thoth.server.database.tables.BookFileMetadataTable
import io.thoth.server.database.tables.BookUserMetadataTable
import io.thoth.server.database.tables.ImageTable
import io.thoth.server.database.tables.layer
import io.thoth.server.database.tables.write
import io.thoth.server.file.analyzer.AudioFileAnalysisResultImpl
import io.thoth.server.file.scanner.LibraryRoots
import io.thoth.server.newLibrary
import io.thoth.server.pngBytes
import io.thoth.server.registerWithAccess
import io.thoth.server.thothServer
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.mp.KoinPlatform.getKoin
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import kotlin.io.path.absolutePathString
import kotlin.io.path.isDirectory
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class TrackManagerTest : ThothTest() {
    private val trackManager by lazy { getKoin().get<TrackManager>() }
    private val roots by lazy { getKoin().get<LibraryRoots>() }
    private var libId: UUID = UUID.randomUUID()

    private fun imported(block: suspend ApplicationTestBuilder.(Headers) -> Unit) =
        thothServer { block(bearer(registerWithAccess("reader", libId, LibraryPermissionLevel.READONLY))) }

    private suspend fun ApplicationTestBuilder.theBook(token: Headers): BookDetailed {
        val listed = api
            .listBooks(libId, headers = token)
            .body()
            .items
            .single()
        return api.getBook(listed.id, libId, token).body()
    }

    private suspend fun ApplicationTestBuilder.theSeries(token: Headers): SeriesDetailed {
        val listed = api
            .listSeries(libId, headers = token)
            .body()
            .items
            .single()
        return api.getSeries(listed.id, libId, token).body()
    }

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
    fun `adding a file links its book, author and series`() =
        imported { token ->
            addPath(bookWithSeries)

            val book = theBook(token)
            assertEquals(1, book.tracks.size, "the track must be imported")
            assertEquals(listOf("Dan Brown"), book.authors.map { it.name }, "the book must be linked to its author")
            assertEquals(listOf("Robert Langdon"), book.series.map { it.title }, "and to its series")
        }

    @Test
    fun `a series dropped from the tags is dropped from the book`() =
        imported { token ->
            trackManager.insert(taggedWithSeries("Robert Langdon"), libId)
            assertEquals(
                listOf("Robert Langdon"),
                theBook(token).series.map { it.title },
                "sanity: the tags put the book in a series",
            )

            trackManager.insert(taggedWithSeries(null), libId)

            assertEquals(
                emptyList(),
                theBook(token).series.map { it.title },
                "retagging the file must unlink the series",
            )
        }

    @Test
    fun `genres from the tags land on the book and on its series`() =
        imported { token ->
            trackManager.insert(taggedWith("Fantasy", "Sci-Fi"), libId)

            assertEquals(setOf("Fantasy", "Sci-Fi"), theBook(token).genres.toSet())
            assertEquals(setOf("Fantasy", "Sci-Fi"), theSeries(token).genres.toSet())
        }

    @Test
    fun `a genre spelled differently by a second file is not duplicated`() =
        imported { token ->
            trackManager.insert(taggedWith("Fantasy"), libId)
            trackManager.insert(taggedWith("fantasy", "FANTASY"), libId)

            assertEquals(setOf("fantasy"), theBook(token).genres.toSet(), "the genre must be reused across spellings")
            assertEquals(
                setOf("fantasy"),
                theSeries(token).genres.toSet(),
                "a series shows the genres its books ended up with",
            )
        }

    private fun scanWithCover(
        cover: ByteArray,
        fileName: String = bookWithSeries.absolutePathString(),
    ) = AudioFileAnalysisResultImpl(
        title = "Angels and Demons",
        authors = listOf("Dan Brown"),
        book = "Angels and Demons",
        series = "Robert Langdon",
        durationMs = 60_000,
        path = fileName,
        lastModified = Instant.EPOCH,
        cover = cover,
    )

    @Test
    fun `a rescan does not overwrite a cover that was edited`() =
        imported { token ->
            // TODO once we have a rescan endpoint use the API for this as well...
            trackManager.insert(scanWithCover(pngBytes(1, 2, 3)), libId)
            val bookId = theBook(token).id

            val edited = transaction { getOrCreateImage(pngBytes(9, 9, 9), null)!! }
            transaction {
                BookUserMetadataTable.write(BookUserMetadataTable.layer(bookId).copy(coverID = edited))
            }

            trackManager.insert(scanWithCover(pngBytes(7, 7, 7)), libId)

            assertEquals(edited, theBook(token).coverID, "the file's embedded art must not replace an edited cover")
            val fileCover = transaction { BookFileMetadataTable.layer(bookId).coverID }
            assertNotEquals(edited, fileCover, "the file layer must have taken the new art")
            assertNotNull(fileCover, "and must still name an image")
        }

    @Test
    fun `tracks of one book with the same embedded art store one image`() {
        trackManager.insert(scanWithCover(pngBytes(1, 2, 3), "${bookWithSeries.absolutePathString()}.1"), libId)
        trackManager.insert(scanWithCover(pngBytes(1, 2, 3), "${bookWithSeries.absolutePathString()}.2"), libId)

        assertEquals(
            1L,
            transaction { ImageTable.selectAll().count() },
            "unchanged art must be recognised rather than stored again",
        )
    }

    @Test
    fun `art that changed on disk replaces the cover`() =
        imported { token ->
            trackManager.insert(scanWithCover(pngBytes(1, 2, 3)), libId)
            val first = theBook(token).coverID

            trackManager.insert(scanWithCover(pngBytes(4, 5, 6)), libId)

            assertNotEquals(first, theBook(token).coverID, "re-importing a file with new art must update the cover")
        }

    private fun taggedWithSeries(series: String?) =
        AudioFileAnalysisResultImpl(
            title = "Angels and Demons",
            authors = listOf("Dan Brown"),
            book = "Angels and Demons",
            series = series,
            durationMs = 60_000,
            path = bookWithSeries.absolutePathString(),
            lastModified = Instant.EPOCH,
        )

    // The test files carry no tags at all, so a scan result is handed to the importer directly
    private fun taggedWith(vararg genres: String) =
        AudioFileAnalysisResultImpl(
            title = "Angels and Demons",
            authors = listOf("Dan Brown"),
            book = "Angels and Demons",
            series = "Robert Langdon",
            durationMs = 60_000,
            path = bookWithSeries.absolutePathString(),
            lastModified = Instant.EPOCH,
            genres = genres.toList(),
        )

    @Test
    fun `adding a second file of the same book reuses the author and series`() =
        imported { token ->
            addPath(bookWithSeries)
            val second =
                bookWithSeries.parent
                    .toFile()
                    .walkTopDown()
                    .first { it.isFile && it.extension == "mp3" && it.toPath() != bookWithSeries }
                    .toPath()

            addPath(second)

            assertEquals(2, theBook(token).tracks.size)
            assertEquals(
                1,
                api
                    .listAuthors(libId, headers = token)
                    .body()
                    .items.size,
                "the author is not duplicated",
            )
            assertEquals(
                1,
                api
                    .listSeries(libId, headers = token)
                    .body()
                    .items.size,
                "nor is the series",
            )
        }
}
