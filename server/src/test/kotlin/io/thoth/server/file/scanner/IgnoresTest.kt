package io.thoth.server.file.scanner

import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IgnoresTest {
    private lateinit var root: Path

    @BeforeTest
    fun setup() {
        root = createTempDirectory("thoth-ignores-test")
    }

    @AfterTest
    fun teardown() {
        root.toFile().deleteRecursively()
    }

    private fun dir(relative: String): Path = root.resolve(relative).also { it.createDirectories() }

    private fun mark(folder: Path) = folder.resolve(IGNORE_FILE).createFile()

    @Test
    fun `a folder without a marker is not ignored`() {
        val book = dir("author/book")
        assertFalse(isIgnored(book, root))
        assertFalse(isIgnored(book.resolve("track.mp3"), root))
    }

    @Test
    fun `a marker ignores the folder holding it`() {
        val book = dir("author/book")
        mark(book)
        assertTrue(isIgnored(book, root))
    }

    @Test
    fun `a marker ignores everything below it`() {
        val book = dir("author/book")
        mark(dir("author"))
        assertTrue(isIgnored(book, root), "a book under a marked author is ignored")
        assertTrue(isIgnored(book.resolve("track.mp3"), root), "a file under a marked author is ignored")
    }

    @Test
    fun `a sibling of a marked folder is unaffected`() {
        mark(dir("author/ignored"))
        assertFalse(isIgnored(dir("author/kept").resolve("track.mp3"), root))
    }

    @Test
    fun `a marker above the root is not honoured`() {
        val library = dir("library")
        mark(root)
        assertFalse(isIgnored(library.resolve("track.mp3"), library), "the search stops at the library root")
    }
}
