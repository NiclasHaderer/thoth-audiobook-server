package io.thoth.metadata

import kotlin.test.Test
import kotlin.test.assertEquals

class NarratorFirstTest {
    private fun hits(vararg narrators: Pair<String, String>) =
        narrators.map { (id, narrator) -> searchHit(id, narrators = listOf(narrator)) }

    private fun order(
        narrator: String?,
        vararg narrators: Pair<String, String>,
    ) = hits(*narrators).narratorFirst(narrator).map { it.id.itemID }

    @Test
    fun `the edition read by the narrator comes first`() {
        assertEquals(listOf("fry", "dale"), order("Stephen Fry", "dale" to "Jim Dale", "fry" to "Stephen Fry"))
    }

    @Test
    fun `a middle name in the tag does not hide the edition`() {
        assertEquals(listOf("fry", "dale"), order("Stephen Fry", "dale" to "Jim Dale", "fry" to "Stephen John Fry"))
    }

    @Test
    fun `a name the provider lists surname first still matches`() {
        assertEquals(listOf("fry", "dale"), order("Stephen Fry", "dale" to "Jim Dale", "fry" to "Fry, Stephen"))
    }

    @Test
    fun `a typo in the tag does not hide the edition`() {
        assertEquals(listOf("fry", "dale"), order("Stephen Fry", "dale" to "Jim Dale", "fry" to "Stephan Fry"))
    }

    @Test
    fun `a narrator nobody read for leaves the order alone`() {
        assertEquals(listOf("dale", "fry"), order("Nobody At All", "dale" to "Jim Dale", "fry" to "Stephen Fry"))
    }

    @Test
    fun `without a narrator the order is left alone`() {
        assertEquals(listOf("dale", "fry"), order(null, "dale" to "Jim Dale", "fry" to "Stephen Fry"))
    }
}
