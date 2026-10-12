package io.thoth.server.api

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.plugins.websocket.ClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.thoth.client.gen.models.LibraryPermissionLevel
import io.thoth.models.ChangeEvent
import io.thoth.models.ChangeEventBatch
import io.thoth.models.ChangeType
import io.thoth.models.ChangedEntity
import io.thoth.models.FileScanner
import io.thoth.server.ThothTest
import io.thoth.server.api
import io.thoth.server.bearer
import io.thoth.server.common.extensions.canonical
import io.thoth.server.config.ThothConfig
import io.thoth.server.di.serialization.JacksonSerialization
import io.thoth.server.file.scanner.LibraryImportPipeline
import io.thoth.server.grant
import io.thoth.server.login
import io.thoth.server.newBook
import io.thoth.server.newLibrary
import io.thoth.server.registerWithAccess
import io.thoth.server.repositories.AuthorRepository
import io.thoth.server.repositories.BookRepository
import io.thoth.server.revoke
import io.thoth.server.thothServer
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.koin.mp.KoinPlatform.getKoin
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.absolutePathString
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.isDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.fail
import kotlin.time.Duration.Companion.seconds

class EventSocketTest : ThothTest() {
    override fun configure(dataDir: Path) = ThothConfig(dataDir = dataDir, settleMillis = 50)

    private suspend fun ApplicationTestBuilder.events(
        token: String?,
        block: suspend ClientWebSocketSession.() -> Unit,
    ) = createClient { install(WebSockets) }.webSocket(
        "/api/events",
        request = { if (token != null) headers.appendAll(bearer(token)) },
        block = block,
    )

    private suspend fun ClientWebSocketSession.nextBatch(): List<ChangeEvent> {
        val frame = withTimeout(5.seconds) { incoming.receive() } as Frame.Text
        return getKoin()
            .get<JacksonSerialization>()
            .objectMapper
            .readValue<ChangeEventBatch>(frame.readText())
            .events
    }

    @Test
    fun `a commit reaches a connected client after the empty batch that says it is listening`() =
        thothServer {
            val libId = newLibrary("lib")

            events(login("admin")) {
                assertEquals(emptyList(), nextBatch())

                val bookId = newBook("Dune", libId)

                assertEquals(newBookIn(libId, bookId), nextBatch())
            }
        }

    private fun newBookIn(
        libraryId: UUID,
        bookId: UUID,
    ) = listOf(
        ChangeEvent(ChangedEntity.BOOK, ChangeType.CREATED, bookId, libraryId),
        ChangeEvent(ChangedEntity.LIBRARY, ChangeType.UPDATED, libraryId, libraryId),
    )

    @Test
    fun `a client stops being told about a library once its access is revoked`() =
        thothServer {
            val revoked = newLibrary("revoked")
            val kept = newLibrary("kept")
            val token = registerWithAccess("listener", revoked, LibraryPermissionLevel.READONLY)
            grant("listener", kept, LibraryPermissionLevel.READONLY)

            events(token) {
                assertEquals(emptyList(), nextBatch())
                revoke("listener", revoked)

                newBook("Hidden", revoked)
                val bookId = newBook("Dune", kept)

                assertEquals(newBookIn(kept, bookId), nextBatch())
            }
        }

    @Test
    fun `a client is only told about the libraries it can read`() =
        thothServer {
            val readable = newLibrary("readable")
            val other = newLibrary("other")
            val token = registerWithAccess("listener", readable, LibraryPermissionLevel.READONLY)

            events(token) {
                assertEquals(emptyList(), nextBatch())

                newBook("Hidden", other)
                val bookId = newBook("Dune", readable)

                assertEquals(newBookIn(readable, bookId), nextBatch())
            }
        }

    private suspend fun ClientWebSocketSession.awaitEvents(vararg expected: ChangeEvent) {
        val missing = expected.toMutableSet()
        while (missing.isNotEmpty()) {
            val batch =
                try {
                    nextBatch()
                } catch (e: TimeoutCancellationException) {
                    fail("never received $missing", e)
                }
            missing -= batch.toSet()
        }
    }

    private val sourceMp3: Path =
        generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .map { it.resolve("test-resources") }
            .first { it.isDirectory() }
            .resolve("Terry Pratchett/Good Omens/Good Omens.mp3")

    @Test
    fun `a book is announced when its file is scanned, removed and put back`() =
        thothServer {
            val root = dataDir.resolve("library")
            val file = root.resolve("Terry Pratchett/Good Omens/Good Omens.mp3")
            val libId =
                newLibrary(
                    "lib",
                    folders = listOf(root.createDirectories().absolutePathString()),
                    fileScanners = listOf(FileScanner("AudioFolderScanner")),
                )
            val pipeline = getKoin().get<LibraryImportPipeline>()

            events(login("admin")) {
                assertEquals(emptyList(), nextBatch())

                sourceMp3.copyTo(file.createParentDirectories())
                pipeline.scanLibrary(libId)
                val author = getKoin().get<AuthorRepository>().findByTaggedName("Terry Pratchett", libId)!!.id
                val book = getKoin().get<BookRepository>().findByTaggedName("Good Omens", listOf(author), libId)!!.id
                awaitEvents(
                    ChangeEvent(ChangedEntity.BOOK, ChangeType.CREATED, book, libId),
                    ChangeEvent(ChangedEntity.AUTHOR, ChangeType.CREATED, author, libId),
                )

                // The rows are only hidden at this point, which to a client is the same as gone
                val watched = file.canonical()
                file.deleteExisting()
                pipeline.enqueue(watched)
                awaitEvents(
                    ChangeEvent(ChangedEntity.BOOK, ChangeType.DELETED, book, libId),
                    ChangeEvent(ChangedEntity.AUTHOR, ChangeType.DELETED, author, libId),
                )
                assertEquals(HttpStatusCode.NotFound, api.getBook(book, libId, bearer(login("admin"))).status)

                sourceMp3.copyTo(file)
                pipeline.enqueue(watched)
                awaitEvents(
                    ChangeEvent(ChangedEntity.BOOK, ChangeType.CREATED, book, libId),
                    ChangeEvent(ChangedEntity.AUTHOR, ChangeType.CREATED, author, libId),
                )
            }
        }

    @Test
    fun `a client without a token is turned away`() =
        thothServer {
            assertFails { events(token = null) {} }
            assertFails { events(token = "not.a.token") {} }
        }
}
