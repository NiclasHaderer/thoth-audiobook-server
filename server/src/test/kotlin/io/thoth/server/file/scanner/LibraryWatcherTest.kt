package io.thoth.server.file.scanner

import io.thoth.models.FileScanner
import io.thoth.models.NamedMetadataAgent
import io.methvin.watcher.DirectoryChangeEvent
import io.thoth.server.ThothTest
import io.thoth.server.database.tables.BookEntity
import io.thoth.server.database.tables.LibraryEntity
import io.thoth.server.database.tables.TrackEntity
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.absolutePathString
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteRecursively
import kotlin.io.path.isDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class LibraryWatcherTest : ThothTest() {
    private val watcher = LibraryWatcherImpl(debounce = 100.milliseconds)

    private lateinit var libraryRoot: Path
    private var libId: UUID = UUID.randomUUID()

    private val sourceMp3: Path =
        generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .map { it.resolve("test-resources") }
            .first { it.isDirectory() }
            .resolve("Terry Pratchett/Good Omens/Good Omens.mp3")

    @BeforeTest
    fun startWatcher() {
        libraryRoot = dataDir.resolve("library").also { it.createDirectories() }
        libId =
            transaction {
                LibraryEntity
                    .new {
                        name = "lib"
                        folders = listOf(libraryRoot.absolutePathString())
                        metadataAgents = listOf(NamedMetadataAgent("audible"))
                        fileScanners = listOf(FileScanner("AudioTagScanner"), FileScanner("AudioFolderScanner"))
                        language = "en"
                    }.id
                    .value
            }
        watcher.start()
        // The watcher registers asynchronously, so wait until it is actually listening
        Thread.sleep(500)
    }

    @AfterTest
    fun stopWatcher() = watcher.stop()

    private fun addBook(
        author: String,
        title: String,
    ): Path {
        val folder = libraryRoot.resolve(author).resolve(title).also { it.createDirectories() }
        sourceMp3.copyTo(folder.resolve("$title.mp3"), overwrite = true)
        return folder
    }

    private fun titles() = transaction { BookEntity.all().map { it.title }.sorted() }

    private fun tracks() = transaction { TrackEntity.all().count() }

    private fun eventually(
        timeout: Duration = 10.seconds,
        describe: () -> String,
        until: () -> Boolean,
    ) = runBlocking {
        val deadline = System.nanoTime() + timeout.inWholeNanoseconds
        while (System.nanoTime() < deadline) {
            if (until()) return@runBlocking
            delay(50)
        }
        throw AssertionError("Timed out waiting for: ${describe()}")
    }

    @Test
    fun `a new file is imported without a rescan`() {
        addBook("An Author", "A Book")

        eventually(describe = { "the book to be imported, saw ${titles()}" }) { titles() == listOf("A Book") }
    }

    @Test
    fun `a deleted file is removed`() {
        val folder = addBook("An Author", "A Book")
        eventually(describe = { "the book to import first" }) { titles() == listOf("A Book") }

        @OptIn(kotlin.io.path.ExperimentalPathApi::class)
        folder.deleteRecursively()

        eventually(describe = { "the book to be dropped, saw ${titles()}" }) { titles().isEmpty() }
    }

    @Test
    fun `a burst of files is coalesced into one import pass`() {
        repeat(5) { index -> addBook("An Author", "Book $index") }

        eventually(describe = { "all five books, saw ${titles()}" }) { tracks() == 5L }
        assertEquals(5, titles().size)
    }

    @Test
    fun `adding an ignore marker invalidates the books already imported`() {
        addBook("An Author", "A Book")
        eventually(describe = { "the book to import first" }) { titles() == listOf("A Book") }

        libraryRoot.resolve("An Author").resolve(IGNORE_FILE).createFile()

        eventually(describe = { "the book to be invalidated, saw ${titles()}" }) { titles().isEmpty() }
        assertEquals(0L, tracks(), "its track must go too")
    }

    @Test
    fun `removing the ignore marker brings the books back`() {
        addBook("An Author", "A Book")
        eventually(describe = { "the book to import first" }) { titles() == listOf("A Book") }
        val marker = libraryRoot.resolve("An Author").resolve(IGNORE_FILE).createFile()
        eventually(describe = { "the book to be invalidated" }) { titles().isEmpty() }

        marker.deleteExisting()

        eventually(describe = { "the book to come back, saw ${titles()}" }) { titles() == listOf("A Book") }
    }

    @Test
    fun `an overflow event rescans the root it came from`() {
        // Written straight to disk with the watcher deliberately not consulted, the way a dropped event
        // leaves things: nothing is queued, so only the overflow handling can bring the book in.
        watcher.stop()
        addBook("An Author", "A Book")
        assertEquals(emptyList(), titles(), "sanity: nothing imported while the watcher was down")

        // The root the library reports is the one it registered, which is already resolved
        watcher.onEvent(
            DirectoryChangeEvent(
                DirectoryChangeEvent.EventType.OVERFLOW,
                false,
                null,
                null,
                1,
                realPath(libraryRoot),
            ),
        )

        eventually(describe = { "the overflow rescan, saw ${titles()}" }) { titles() == listOf("A Book") }
    }

    @Test
    fun `an overflow event without a root is ignored`() {
        watcher.onEvent(
            DirectoryChangeEvent(DirectoryChangeEvent.EventType.OVERFLOW, false, null, null, 1, null),
        )

        Thread.sleep(300)
        assertEquals(emptyList(), titles())
    }

    @Test
    fun `concurrent restarts leave no watcher behind after stop`() {
        List(8) { Thread { watcher.restart() }.also { it.start() } }.forEach { it.join() }
        Thread.sleep(1000)

        watcher.stop()

        addBook("An Author", "A Book")
        Thread.sleep(2000)
        assertEquals(emptyList(), titles(), "a leaked watcher would still be importing")
    }

    @Test
    fun `a file added under an ignored folder is never imported`() {
        libraryRoot
            .resolve("An Author")
            .also { it.createDirectories() }
            .resolve(IGNORE_FILE)
            .createFile()
        Thread.sleep(300)

        addBook("An Author", "A Book")

        // Nothing should ever show up, so give the watcher longer than a passing case would need
        Thread.sleep(2000)
        assertEquals(emptyList(), titles(), "the marked folder must stay out of the library")
    }
}
