package io.thoth.server.common.exposed

import io.thoth.server.ThothTest
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.LibrariesTable
import io.thoth.server.newAuthor
import io.thoth.server.newLibrary
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private object AuthorDisplayView : View("AuthorDisplay") {
    val id = javaUUID("id")
    val displayedName = text("displayedName")
    val library = javaUUID("library")

    override fun body() =
        AuthorTable.select(
            AuthorTable.id,
            AuthorTable.displayedName.alias("displayedName"),
            AuthorTable.library,
        )
}

private object LowercaseAuthorView : View("LowercaseAuthor") {
    val id = javaUUID("id")
    val displayedName = text("displayedName")

    override fun body() =
        AuthorDisplayView.select(
            AuthorDisplayView.id,
            AuthorDisplayView.displayedName.lowerCase().alias("displayedName"),
        )
}

private object EnglishAuthorView : View("EnglishAuthor") {
    val id = javaUUID("id")
    val name = text("name")

    override fun body() =
        (AuthorTable innerJoin LibrariesTable)
            .select(AuthorTable.id, AuthorTable.name)
            .where { LibrariesTable.language eq "en" }
}

private object MismatchedArityView : View("MismatchedArity") {
    val id = javaUUID("id")
    val name = text("name")

    override fun body() = AuthorTable.select(AuthorTable.id)
}

// Same arity, same types, but the two text columns are declared in the opposite order to the body
private object SwappedCompatibleView : View("SwappedCompatible") {
    val website = text("website")
    val name = text("name")

    override fun body() = AuthorTable.select(AuthorTable.name, AuthorTable.website)
}

private object UnaliasedView : View("Unaliased") {
    val id = javaUUID("id")
    val displayedName = text("displayedName")

    override fun body() = AuthorTable.select(AuthorTable.id, AuthorTable.displayedName)
}

// Same arity, but a uuid column sits where the body selects text and vice versa
private object SwappedIncompatibleView : View("SwappedIncompatible") {
    val id = javaUUID("id")
    val name = text("name")

    override fun body() = AuthorTable.select(AuthorTable.name, AuthorTable.id)
}

class ViewTest : ThothTest() {
    private lateinit var libId: UUID

    private fun createLibrary() {
        libId = newLibrary("lib")
    }

    @Test
    fun `view resolves a coalesce expression`() {
        createLibrary()
        newAuthor("Plain", libId)
        newAuthor("Raw", libId, displayName = "Pretty")
        syncViews(listOf(AuthorDisplayView))

        val names =
            transaction {
                AuthorDisplayView
                    .selectAll()
                    .where { AuthorDisplayView.library eq libId }
                    .orderBy(AuthorDisplayView.displayedName to SortOrder.ASC)
                    .map { it[AuthorDisplayView.displayedName] }
            }

        assertEquals(listOf("Plain", "Pretty"), names)
    }

    @Test
    fun `view inlines the predicate of a joined body`() {
        createLibrary()
        newAuthor("Tolkien", libId)
        syncViews(listOf(EnglishAuthorView))

        val ddl = transaction { EnglishAuthorView.createStatement().single() }
        assertContains(ddl, "'en'")

        val names = transaction { EnglishAuthorView.selectAll().map { it[EnglishAuthorView.name] } }
        assertEquals(listOf("Tolkien"), names)
    }

    @Test
    fun `syncing twice leaves a readable view`() {
        createLibrary()
        newAuthor("Plain", libId)
        syncViews(listOf(AuthorDisplayView))
        syncViews(listOf(AuthorDisplayView))

        assertEquals(1, transaction { AuthorDisplayView.selectAll().count() })
    }

    @Test
    fun `a view built on another view is created after it`() {
        createLibrary()
        newAuthor("Raw", libId, displayName = "Pretty")
        // Reverse order on purpose: the dependency has to be pulled forward
        syncViews(listOf(LowercaseAuthorView, AuthorDisplayView))

        val names = transaction { LowercaseAuthorView.selectAll().map { it[LowercaseAuthorView.displayedName] } }
        assertEquals(listOf("pretty"), names)
    }

    @Test
    fun `a column list that disagrees with the body is rejected`() {
        val error = assertFailsWith<IllegalStateException> { syncViews(listOf(MismatchedArityView)) }
        assertEquals("View MismatchedArity declares 2 columns but its body selects 1", error.message)
    }

    // The column list binds positionally, and neither SQLite nor Exposed catches a wrong order: compatible
    // types just swap values, and a text column read as a UUID either underflows or decodes to garbage
    @Test
    fun `columns declared out of select order are rejected`() {
        val compatible = assertFailsWith<IllegalStateException> { syncViews(listOf(SwappedCompatibleView)) }
        assertEquals(
            "View SwappedCompatible declares column 'website' where its body selects 'name'. " +
                "Columns must be declared in select order",
            compatible.message,
        )

        val incompatible = assertFailsWith<IllegalStateException> { syncViews(listOf(SwappedIncompatibleView)) }
        assertEquals(
            "View SwappedIncompatible declares column 'id' where its body selects 'name'. " +
                "Columns must be declared in select order",
            incompatible.message,
        )
    }

    @Test
    fun `an unnamed expression in the body is rejected`() {
        val error = assertFailsWith<IllegalStateException> { syncViews(listOf(UnaliasedView)) }
        assertEquals(
            "View Unaliased selects an unnamed expression for column 'displayedName'. " +
                "Add .alias(\"displayedName\") to it",
            error.message,
        )
    }
}
