package io.thoth.server.file.scanner

import io.thoth.models.FileScanner
import io.thoth.server.ThothTest
import io.thoth.server.common.extensions.canonical
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.TracksTable
import io.thoth.server.newLibrary
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.mp.KoinPlatform.getKoin
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.absolutePathString
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.deleteExisting
import kotlin.io.path.isDirectory
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryScannerIgnoreTest : ThothTest() {
    private val pipeline by lazy { getKoin().get<LibraryImportPipeline>() }

    private lateinit var libraryRoot: Path
    private var libId: UUID = UUID.randomUUID()

    private val sourceMp3: Path =
        generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .map { it.resolve("test-resources") }
            .first { it.isDirectory() }
            .resolve("Terry Pratchett/Good Omens/Good Omens.mp3")

    @BeforeTest
    fun createLibrary() {
        libraryRoot = dataDir.resolve("library").also { it.createDirectories() }
        book("Kept Author", "Kept Book")
        book("Dropped Author", "Dropped Book")
        libId =
            newLibrary(
                "lib",
                folders = listOf(libraryRoot.absolutePathString()),
                fileScanners = listOf(FileScanner("AudioTagScanner"), FileScanner("AudioFolderScanner")),
            )
    }

    private fun book(
        author: String,
        title: String,
    ): Path {
        val folder = libraryRoot.resolve(author).resolve(title).also { it.createDirectories() }
        sourceMp3.copyTo(folder.resolve("$title.mp3"), overwrite = true)
        return folder
    }

    private fun scan() = pipeline.scanLibrary(libId)

    private fun titles() = transaction { BooksTable.selectAll().map { it[BooksTable.title] }.sorted() }

    private fun counts() =
        transaction {
            Triple(
                TracksTable.selectAll().count(),
                BooksTable.selectAll().count(),
                AuthorTable.selectAll().count(),
            )
        }

    @Test
    fun `a folder marked before the first scan is never imported`() {
        libraryRoot.resolve("Dropped Author").resolve(IGNORE_FILE).createFile()

        scan()

        assertEquals(listOf("Kept Book"), titles())
    }

    @Test
    fun `marking a folder invalidates the books already imported from it`() {
        scan()
        assertEquals(listOf("Dropped Book", "Kept Book"), titles(), "sanity: both books import first")

        libraryRoot.resolve("Dropped Author").resolve(IGNORE_FILE).createFile()
        scan()

        assertEquals(listOf("Kept Book"), titles(), "the book behind the new marker must be dropped")
        assertEquals(
            Triple(1L, 1L, 1L),
            counts(),
            "its track and its now bookless author must be reaped too",
        )
    }

    @Test
    fun `removing the marker brings the books back`() {
        val marker = libraryRoot.resolve("Dropped Author").resolve(IGNORE_FILE).createFile()
        scan()
        assertEquals(listOf("Kept Book"), titles(), "sanity: the marked book is absent")

        marker.deleteExisting()
        scan()

        assertEquals(listOf("Dropped Book", "Kept Book"), titles())
    }

    @Test
    fun `a file added inside an ignored folder is not imported`() {
        libraryRoot.resolve("Dropped Author").resolve(IGNORE_FILE).createFile()
        scan()

        book("Dropped Author", "Sneaky Book")
        scan()

        assertEquals(listOf("Kept Book"), titles())
    }

    @Test
    fun `a subtree walk started below a marked folder still honours the marker`() {
        scan()
        libraryRoot.resolve("Dropped Author").resolve(IGNORE_FILE).createFile()
        val sneaky = book("Dropped Author", "Sneaky Book")

        pipeline.enqueue(sneaky.canonical())

        // Nothing should ever show up, so give the pool longer than a passing case would need
        Thread.sleep(2000)
        assertEquals(listOf("Dropped Book", "Kept Book"), titles(), "the new book must not be imported")
    }
}
