package io.thoth.server.file.tagger

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReadonlyFileTaggerTest {
    private fun taglibFixture(name: String): Path =
        generateSequence(Path.of("").toAbsolutePath()) { it.parent }
            .map { it.resolve("taglib/src/test/resources/$name") }
            .first { it.exists() }

    @Test
    fun `reads the chapters and the exact length of an m4b`() {
        val tags = ReadonlyFileTagger(taglibFixture("chapters.m4b"))

        assertEquals(listOf("Chapter One", "Chapter Two", "Chapter Three"), tags.chapters.map { it.title })
        assertEquals(listOf(0L, 2000L, 4000L), tags.chapters.map { it.startMs })
        assertTrue(tags.durationMs > 4000, "the length must cover the last chapter, was ${tags.durationMs}ms")
    }
}
