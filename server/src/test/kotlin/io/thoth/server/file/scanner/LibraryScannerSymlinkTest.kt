package io.thoth.server.file.scanner

import io.thoth.models.FileScanner
import io.thoth.server.ThothTest
import io.thoth.server.database.tables.BookTable
import io.thoth.server.database.tables.TrackTable
import io.thoth.server.newLibrary
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.mp.KoinPlatform.getKoin
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.absolutePathString
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.isDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryScannerSymlinkTest : ThothTest() {
    private val pipeline by lazy { getKoin().get<LibraryImportPipeline>() }

    private val sourceMp3: Path =
        generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .map { it.resolve("test-resources") }
            .first { it.isDirectory() }
            .resolve("Terry Pratchett/Good Omens/Good Omens.mp3")

    private fun book(
        parent: Path,
        author: String,
        title: String,
    ): Path {
        val folder = parent.resolve(author).resolve(title).also { it.createDirectories() }
        sourceMp3.copyTo(folder.resolve("$title.mp3"), overwrite = true)
        return folder
    }

    private fun createLibrary(root: Path): UUID =
        newLibrary(
            "lib",
            folders = listOf(root.absolutePathString()),
            fileScanners = listOf(FileScanner("AudioTagScanner"), FileScanner("AudioFolderScanner")),
        )

    private fun scan(id: UUID) = pipeline.scanLibrary(id)

    private fun titles() = transaction { BookTable.selectAll().mapNotNull { it[BookTable.title] }.sorted() }

    @Test
    fun `a symlinked folder inside the library is not descended into`() {
        val root = dataDir.resolve("library").also { it.createDirectories() }
        val outside = dataDir.resolve("outside").also { it.createDirectories() }
        book(root, "Real Author", "Real Book")
        book(outside, "Linked Author", "Linked Book")
        Files.createSymbolicLink(root.resolve("Linked Author"), outside.resolve("Linked Author"))

        scan(createLibrary(root))

        assertEquals(listOf("Real Book"), titles())
    }

    @Test
    fun `a link to a folder already in the library does not import it twice`() {
        val root = dataDir.resolve("library").also { it.createDirectories() }
        book(root, "Real Author", "Real Book")
        Files.createSymbolicLink(root.resolve("Alias"), root.resolve("Real Author"))

        scan(createLibrary(root))

        assertEquals(listOf("Real Book"), titles())
        assertEquals(1L, transaction { TrackTable.selectAll().count() })
    }

    @Test
    fun `a library whose root is a symlink is still scanned`() {
        val real = dataDir.resolve("real").also { it.createDirectories() }
        book(real, "Real Author", "Real Book")
        val link = Files.createSymbolicLink(dataDir.resolve("library"), real)

        scan(createLibrary(link))

        assertEquals(listOf("Real Book"), titles())
    }
}
