package io.thoth.server.file

import io.thoth.models.FileScanner
import io.thoth.models.NamedMetadataAgent
import io.thoth.server.database.tables.AuthorBookTable
import io.thoth.server.database.tables.AuthorEntity
import io.thoth.server.database.tables.BookEntity
import io.thoth.server.database.tables.LibraryEntity
import io.thoth.server.database.tables.SeriesAuthorTable
import io.thoth.server.database.tables.SeriesBookTable
import io.thoth.server.database.tables.SeriesEntity
import io.thoth.server.database.tables.TrackEntity
import io.thoth.server.ThothTest
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.absolutePathString
import kotlin.io.path.isDirectory
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TrackManagerTest : ThothTest() {
    private var libId: UUID = UUID.randomUUID()

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
        TrackManager.addPath(bookWithSeries, transaction { LibraryEntity[libId] })

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
    fun `adding a second file of the same book reuses the author and series`() {
        val library = transaction { LibraryEntity[libId] }
        TrackManager.addPath(bookWithSeries, library)
        val second =
            bookWithSeries.parent
                .toFile()
                .walkTopDown()
                .first { it.isFile && it.extension == "mp3" && it.toPath() != bookWithSeries }
                .toPath()

        TrackManager.addPath(second, library)

        transaction {
            assertEquals(2L, TrackEntity.all().count())
            assertEquals(1L, AuthorEntity.all().count(), "the author must not be duplicated")
            assertEquals(1L, SeriesEntity.all().count(), "the series must not be duplicated")
            assertEquals(1L, SeriesAuthorTable.selectAll().count())
        }
    }
}
