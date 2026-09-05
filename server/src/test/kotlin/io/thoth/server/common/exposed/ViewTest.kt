package io.thoth.server.common.exposed

import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.server.ThothTest
import io.thoth.server.database.tables.AuthorFileMetadataTable
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.AuthorUserMetadataTable
import io.thoth.server.database.tables.LibrariesTable
import io.thoth.server.newAuthor
import io.thoth.server.newLibrary
import org.jetbrains.exposed.v1.core.Coalesce
import org.jetbrains.exposed.v1.core.JoinType
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

private object ResolvedNameView : View("ResolvedName") {
    val id = javaUUID("id")
    val resolvedName = text("resolvedName")
    val library = javaUUID("library")

    override fun body() =
        AuthorTable
            .join(AuthorFileMetadataTable, JoinType.LEFT, AuthorTable.id, AuthorFileMetadataTable.id)
            .join(AuthorUserMetadataTable, JoinType.LEFT, AuthorTable.id, AuthorUserMetadataTable.id)
            .select(
                AuthorTable.id,
                Coalesce(AuthorUserMetadataTable.name, AuthorFileMetadataTable.name).alias("resolvedName"),
                AuthorTable.library,
            )
}

private object LowercaseAuthorView : View("LowercaseAuthor") {
    val id = javaUUID("id")
    val resolvedName = text("resolvedName")

    override fun body() =
        ResolvedNameView.select(
            ResolvedNameView.id,
            ResolvedNameView.resolvedName.lowerCase().alias("resolvedName"),
        )
}

private object EnglishAuthorView : View("EnglishAuthor") {
    val id = javaUUID("id")
    val name = text("name")

    override fun body() =
        (AuthorTable innerJoin LibrariesTable innerJoin AuthorFileMetadataTable)
            .select(AuthorTable.id, AuthorFileMetadataTable.name)
            .where { LibrariesTable.language eq MetadataLanguage.English }
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

    override fun body() = AuthorFileMetadataTable.select(AuthorFileMetadataTable.name, AuthorFileMetadataTable.website)
}

private object UnaliasedView : View("Unaliased") {
    val id = javaUUID("id")
    val resolvedName = text("resolvedName")

    override fun body() =
        AuthorTable
            .join(AuthorFileMetadataTable, JoinType.LEFT, AuthorTable.id, AuthorFileMetadataTable.id)
            .join(AuthorUserMetadataTable, JoinType.LEFT, AuthorTable.id, AuthorUserMetadataTable.id)
            .select(
                AuthorTable.id,
                Coalesce(AuthorUserMetadataTable.name, AuthorFileMetadataTable.name),
            )
}

// Same arity, but a uuid column sits where the body selects text and vice versa
private object SwappedIncompatibleView : View("SwappedIncompatible") {
    val id = javaUUID("id")
    val name = text("name")

    override fun body() =
        (AuthorTable innerJoin AuthorFileMetadataTable).select(AuthorFileMetadataTable.name, AuthorTable.id)
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
        newAuthor("Raw", libId, renamedTo = "Pretty")
        syncViews(listOf(ResolvedNameView))

        val names =
            transaction {
                ResolvedNameView
                    .selectAll()
                    .where { ResolvedNameView.library eq libId }
                    .orderBy(ResolvedNameView.resolvedName to SortOrder.ASC)
                    .map { it[ResolvedNameView.resolvedName] }
            }

        assertEquals(listOf("Plain", "Pretty"), names)
    }

    @Test
    fun `view inlines the predicate of a joined body`() {
        createLibrary()
        newAuthor("Tolkien", libId)
        syncViews(listOf(EnglishAuthorView))

        val ddl = transaction { EnglishAuthorView.createStatement().single() }
        assertContains(ddl, "'English'")

        val names = transaction { EnglishAuthorView.selectAll().map { it[EnglishAuthorView.name] } }
        assertEquals(listOf("Tolkien"), names)
    }

    @Test
    fun `syncing twice leaves a readable view`() {
        createLibrary()
        newAuthor("Plain", libId)
        syncViews(listOf(ResolvedNameView))
        syncViews(listOf(ResolvedNameView))

        assertEquals(1, transaction { ResolvedNameView.selectAll().count() })
    }

    @Test
    fun `a view built on another view is created after it`() {
        createLibrary()
        newAuthor("Raw", libId, renamedTo = "Pretty")
        // Reverse order on purpose: the dependency has to be pulled forward
        syncViews(listOf(LowercaseAuthorView, ResolvedNameView))

        val names = transaction { LowercaseAuthorView.selectAll().map { it[LowercaseAuthorView.resolvedName] } }
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
            "View Unaliased selects an unnamed expression for column 'resolvedName'. " +
                "Add .alias(\"resolvedName\") to it",
            error.message,
        )
    }
}
