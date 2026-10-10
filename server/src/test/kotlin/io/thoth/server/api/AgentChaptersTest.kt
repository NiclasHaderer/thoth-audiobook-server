package io.thoth.server.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.thoth.metadata.FakeMetadataAgent
import io.thoth.metadata.MetadataAgents
import io.thoth.metadata.responses.MetadataChapters
import io.thoth.metadata.searchHit
import io.thoth.models.ChapterMark
import io.thoth.models.NamedMetadataAgent
import io.thoth.server.ThothTest
import io.thoth.server.api
import io.thoth.server.bearer
import io.thoth.server.database.tables.TrackChapter
import io.thoth.server.login
import io.thoth.server.newAuthor
import io.thoth.server.newBook
import io.thoth.server.newLibrary
import io.thoth.server.newTrack
import io.thoth.server.thothServer
import org.koin.dsl.module
import org.koin.mp.KoinPlatform.getKoin
import kotlin.test.Test
import kotlin.test.assertEquals

class AgentChaptersTest : ThothTest() {
    private val agentChapters = listOf(ChapterMark("Prologue", 0), ChapterMark("Arrakis", 2_000))

    private fun useAgent(runtimeMs: Long) {
        val agent =
            FakeMetadataAgent(
                hits = listOf(searchHit("Dune", authors = listOf("Frank Herbert"))),
                resolveChapters = { MetadataChapters(runtimeMs, agentChapters) },
            )
        getKoin().loadModules(listOf(module { single { MetadataAgents(listOf(agent)) } }), allowOverride = true)
    }

    private suspend fun ApplicationTestBuilder.matchedChapterTitles(
        preferEmbeddedMetadata: Boolean = false,
        trackChapters: List<TrackChapter> = emptyList(),
    ): List<String?> {
        val name = "lib-$preferEmbeddedMetadata"
        val libId =
            newLibrary(
                name,
                preferEmbeddedMetadata = preferEmbeddedMetadata,
                metadataAgents = listOf(NamedMetadataAgent("fake")),
            )
        val bookId = newBook("Dune", libId, authors = listOf(newAuthor("Frank Herbert", libId)))
        newTrack("Dune", "/media/$name/dune.m4b", bookId, libId, durationMs = 60_000, chapters = trackChapters)
        val token = bearer(login("admin"))

        assertEquals(HttpStatusCode.OK, api.autoMatchBook(bookId, libId, token).status)

        return api
            .getBook(bookId, libId, token)
            .body()
            .chapters
            .map { it.title }
    }

    @Test
    fun `a match brings its chapters when its runtime fits the files`() =
        thothServer {
            useAgent(runtimeMs = 60_000 + 9_000)

            assertEquals(listOf("Prologue", "Arrakis"), matchedChapterTitles())
        }

    @Test
    fun `a match of another edition keeps the chapters of the files`() =
        thothServer {
            useAgent(runtimeMs = 60_000 + 10 * 60_000)

            assertEquals(listOf("Dune"), matchedChapterTitles(), "one chapter per file, as before the match")
        }

    @Test
    fun `markers in the files win over a match when the library prefers them`() =
        thothServer {
            useAgent(runtimeMs = 60_000)
            val markers = listOf(TrackChapter("One", 0), TrackChapter("Two", 30_000))

            assertEquals(
                listOf("One", "Two"),
                matchedChapterTitles(preferEmbeddedMetadata = true, trackChapters = markers),
            )
            assertEquals(listOf("Prologue", "Arrakis"), matchedChapterTitles(trackChapters = markers))
        }
}
