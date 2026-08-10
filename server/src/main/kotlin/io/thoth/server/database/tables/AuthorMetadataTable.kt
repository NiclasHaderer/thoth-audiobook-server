package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.LocalDate
import java.util.UUID

sealed class AuthorMetadata(
    name: String,
) : IdTable<UUID>(name) {
    final override val id = reference("author", AuthorTable, onDelete = ReferenceOption.CASCADE)
    final override val primaryKey = PrimaryKey(id)

    val name = text("name").nullable()
    val biography = text("biography").nullable()
    val website = varchar("website", 255).nullable()
    val birthDate = date("birthDate").nullable()
    val bornIn = varchar("bornIn", 255).nullable()
    val deathDate = date("deathDate").nullable()
    val provider = varchar("provider", 255).nullable()
    val providerID = varchar("providerID", 255).nullable()
    val imageID = reference("imageId", ImageTable, onDelete = ReferenceOption.SET_NULL).nullable()
}

object AuthorFileMetadataTable : AuthorMetadata("AuthorFileMetadata")

object AuthorAgentMetadataTable : AuthorMetadata("AuthorAgentMetadata")

object AuthorUserMetadataTable : AuthorMetadata("AuthorUserMetadata")

data class AuthorMetadataRow(
    val author: UUID,
    val name: String? = null,
    val biography: String? = null,
    val website: String? = null,
    val birthDate: LocalDate? = null,
    val bornIn: String? = null,
    val deathDate: LocalDate? = null,
    val provider: String? = null,
    val providerID: String? = null,
    val imageID: UUID? = null,
)

context(_: Transaction)
fun AuthorMetadata.layer(authorId: UUID): AuthorMetadataRow =
    selectAll()
        .where { id eq authorId }
        .firstOrNull()
        ?.toAuthorMetadataRow(this)
        ?: AuthorMetadataRow(author = authorId)

private fun ResultRow.toAuthorMetadataRow(table: AuthorMetadata): AuthorMetadataRow =
    AuthorMetadataRow(
        author = this[table.id].value,
        name = this[table.name],
        biography = this[table.biography],
        website = this[table.website],
        birthDate = this[table.birthDate],
        bornIn = this[table.bornIn],
        deathDate = this[table.deathDate],
        provider = this[table.provider],
        providerID = this[table.providerID],
        imageID = this[table.imageID]?.value,
    )

context(_: Transaction)
fun AuthorMetadata.write(row: AuthorMetadataRow) {
    val updated = update({ id eq row.author }) { write(it, row) }
    if (updated == 0) insert { write(it, row) }
}

private fun AuthorMetadata.write(
    stmt: UpdateBuilder<*>,
    row: AuthorMetadataRow,
) {
    stmt[id] = row.author
    stmt[name] = row.name
    stmt[biography] = row.biography
    stmt[website] = row.website
    stmt[birthDate] = row.birthDate
    stmt[bornIn] = row.bornIn
    stmt[deathDate] = row.deathDate
    stmt[provider] = row.provider
    stmt[providerID] = row.providerID
    stmt[imageID] = row.imageID
}
