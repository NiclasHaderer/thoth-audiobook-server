package io.thoth.taglib

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readBytes
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TagLibFileTest {
    @Test
    fun `reads tags and audio properties from mp3`() {
        fixture("chapters-cover.mp3").use { file ->
            assertTrue(file.isValid())
            val properties = file.properties()
            assertEquals(listOf("Chaptered Book"), properties["TITLE"])
            assertEquals(listOf("Test Author"), properties["ARTIST"])
            assertEquals(listOf("Test Album"), properties["ALBUM"])
            assertEquals(6, file.lengthInSeconds)
            assertEquals(22050, file.sampleRate)
            assertEquals(1, file.channels)
            assertTrue(file.bitrate > 0)
        }
    }

    @Test
    fun `reads cover art`() {
        fixture("chapters-cover.mp3").use { file ->
            val pictures = file.pictures()
            assertEquals(1, pictures.size)
            val picture = pictures.single()
            assertEquals("image/png", picture.mimeType)
            // The bytes must survive the copy out of native memory intact.
            assertContentEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47), picture.data.take(4).toByteArray())
            assertTrue(picture.data.size > 4)
        }
    }

    @Test
    fun `reads id3v2 chapters with start and end times`() {
        fixture("chapters-cover.mp3").use { file ->
            val chapters = file.chapters()
            assertEquals(listOf("Chapter One", "Chapter Two", "Chapter Three"), chapters.map { it.title })
            assertEquals(listOf(0L, 2000L, 4000L), chapters.map { it.startMs })
            assertTrue(chapters.all { it.endMs != null }, "ID3v2 CHAP frames carry end times")
            assertEquals(2000L, chapters[0].endMs)
        }
    }

    @Test
    fun `reads mp4 chapters which have no end time`() {
        fixture("chapters.m4b").use { file ->
            val chapters = file.chapters()
            assertEquals(listOf("Chapter One", "Chapter Two", "Chapter Three"), chapters.map { it.title })
            assertEquals(listOf(0L, 2000L, 4000L), chapters.map { it.startMs })
            assertTrue(chapters.all { it.endMs == null }, "MP4 chapters store only start times")
        }
    }

    @Test
    fun `reads chapters stored as vorbis comments`() {
        fixture("chapters.ogg").use { file ->
            val chapters = file.chapters()
            assertEquals(listOf("Chapter One", "Chapter Two", "Chapter Three"), chapters.map { it.title })
            assertEquals(listOf(0L, 2000L, 4000L), chapters.map { it.startMs })
            assertTrue(chapters.all { it.endMs == null })
        }
    }

    @Test
    fun `returns no chapters for a file without any`() {
        fixture("jaudiotagger-tagged.mp3").use { file ->
            assertEquals(emptyList(), file.chapters())
            assertEquals(emptyList(), file.pictures())
        }
    }

    /**
     * Guards the key mapping against files written by the jaudiotagger scheme this module replaced:
     * series went to ID3v2 TIT1, which TagLib reports as WORK (not GROUPING), and the series index
     * went to a TXXX frame reported as CATALOGNUMBER (not CATALOG_NO).
     */
    @Test
    fun `reads tags written by the previous jaudiotagger scheme`() {
        fixture("jaudiotagger-tagged.mp3").use { file ->
            val properties = file.properties()
            assertEquals(listOf("Skulduggery Pleasant"), properties["TITLE"])
            assertEquals(listOf("Derek Landy"), properties["ARTIST"])
            assertEquals(listOf("Skulduggery Pleasant"), properties["ALBUM"])
            assertEquals(listOf("Skulduggery Pleasant"), properties["WORK"])
            assertEquals(listOf("1"), properties["CATALOGNUMBER"])
            assertEquals(listOf("02"), properties["TRACKNUMBER"])
            assertEquals(listOf("2007"), properties["DATE"])
            assertNull(properties["GROUPING"], "series lives in WORK for ID3v2, not GROUPING")
        }
    }

    /** The fixture still carries ffmpeg's ENCODING tag, so only the audiobook keys are absent. */
    @Test
    fun `reports missing keys for a file without tags`() {
        fixture("no-tags.mp3").use { file ->
            assertTrue(file.isValid())
            val properties = file.properties()
            assertNull(properties["TITLE"])
            assertNull(properties["ARTIST"])
            assertNull(properties["ALBUM"])
            assertEquals(emptyList(), file.pictures())
        }
    }

    @Test
    fun `reading does not modify the file`() {
        val path = copyFixture("chapters-cover.mp3")
        val before = path.readBytes()
        TagLibFile(path).use { file ->
            file.properties()
            file.pictures()
            file.chapters()
            file.lengthInSeconds
        }
        assertContentEquals(before, path.readBytes(), "opening a file read-only must not rewrite it")
    }

    @Test
    fun `use after close fails`() {
        val file = fixture("chapters.m4b")
        file.close()
        assertTrue(runCatching { file.properties() }.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `opening a missing file throws`() {
        val directory = createTempDirectory("thoth-taglib-test")
        directory.toFile().deleteOnExit()
        val missing = directory.resolve("nope.mp3")
        assertTrue(runCatching { TagLibFile(missing) }.exceptionOrNull() is TagLibException)
    }

    private fun fixture(name: String) = TagLibFile(copyFixture(name))

    /** TagLib needs a real path, and resources may live inside a jar. */
    private fun copyFixture(name: String): Path {
        val directory = createTempDirectory("thoth-taglib-test")
        val target = directory.resolve(name)
        checkNotNull(javaClass.getResourceAsStream("/$name")) { "missing test fixture $name" }
            .use { Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING) }
        // deleteOnExit runs in reverse registration order, so the directory must be registered first.
        directory.toFile().deleteOnExit()
        target.toFile().deleteOnExit()
        return target
    }
}
