package io.thoth.server.common.extensions

import kotlin.test.Test
import kotlin.test.assertEquals

class NaturalOrderTest {
    private fun sorted(vararg values: String) = values.toList().sortedWith(naturalOrder)

    @Test
    fun `digit runs are compared as numbers, not as text`() {
        assertEquals(
            listOf("Chapter 2.mp3", "Chapter 9.mp3", "Chapter 10.mp3", "Chapter 100.mp3"),
            sorted("Chapter 10.mp3", "Chapter 100.mp3", "Chapter 2.mp3", "Chapter 9.mp3"),
        )
    }

    @Test
    fun `leading zeros do not change the order`() {
        assertEquals(listOf("track 007", "track 8"), sorted("track 8", "track 007"))
    }

    @Test
    fun `numbers in several positions are compared left to right`() {
        assertEquals(
            listOf("d1/t2.mp3", "d1/t10.mp3", "d2/t1.mp3"),
            sorted("d2/t1.mp3", "d1/t10.mp3", "d1/t2.mp3"),
        )
    }

    @Test
    fun `text is compared case insensitively`() {
        assertEquals(listOf("apple", "Banana"), sorted("Banana", "apple"))
    }

    @Test
    fun `a prefix sorts before the longer string`() {
        assertEquals(listOf("Dune", "Dune 2"), sorted("Dune 2", "Dune"))
    }

    @Test
    fun `numbers too large for a long are still ordered`() {
        assertEquals(
            listOf("f99999999999999999999998", "f99999999999999999999999"),
            sorted("f99999999999999999999999", "f99999999999999999999998"),
        )
    }
}
