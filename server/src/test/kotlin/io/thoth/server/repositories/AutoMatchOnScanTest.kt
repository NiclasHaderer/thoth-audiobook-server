package io.thoth.server.repositories

import io.thoth.metadata.FakeMetadataAgent
import io.thoth.metadata.MetadataAgents
import io.thoth.metadata.searchHit
import io.thoth.models.NamedMetadataAgent
import io.thoth.server.ThothTest
import io.thoth.server.database.tables.BookAgentMetadataTable
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.file.scanner.LibraryImportPipeline
import io.thoth.server.newLibrary
import io.thoth.server.schedules.AutoMatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.dsl.module
import org.koin.mp.KoinPlatform.getKoin
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.isDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class AutoMatchOnScanTest : ThothTest() {
    private val pipeline by lazy { getKoin().get<LibraryImportPipeline>() }
    private val autoMatcher by lazy { getKoin().get<AutoMatcher>() }

    private val testResources: Path =
        generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .map { it.resolve("test-resources") }
            .first { it.isDirectory() }

    @BeforeTest
    fun startMatcher() {
        val agent =
            FakeMetadataAgent(
                hits =
                    listOf("Angels and Demons", "Da Vinci Code").map {
                        searchHit(it, authors = listOf("Dan Brown"), series = listOf("Robert Langdon"))
                    },
            )
        getKoin().loadModules(listOf(module { single { MetadataAgents(listOf(agent)) } }), allowOverride = true)
        autoMatcher.start()
    }

    @AfterTest
    fun stopMatcher() {
        autoMatcher.stop()
    }

    @Test
    fun `a new library matches its books against its metadata agent`() {
        val libId =
            newLibrary(
                "fake",
                folders = listOf(testResources.resolve("Dan Brown").absolutePathString()),
                metadataAgents = listOf(NamedMetadataAgent("fake")),
            )

        pipeline.scanLibrary(libId)

        eventually(describe = { "the scan to import a book" }) { books() > 0 }
        eventually(describe = { "the imported books to be matched, ${matched()} of ${books()} are" }) {
            matched() == books()
        }
        assertTrue(
            transaction { BookAgentMetadataTable.selectAll().all { it[BookAgentMetadataTable.provider] != null } },
            "every match must record the agent it came from",
        )
    }

    private fun books() = transaction { BooksTable.selectAll().count() }

    private fun matched() = transaction { BookAgentMetadataTable.selectAll().count() }

    private fun eventually(
        timeout: Duration = 30.seconds,
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
}
