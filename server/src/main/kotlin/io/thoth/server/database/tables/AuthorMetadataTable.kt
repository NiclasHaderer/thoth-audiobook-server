package io.thoth.server.database.tables

import io.thoth.server.database.extensions.json
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

enum class AuthorField : LayerField {
    NAME,
    PROVIDER,
    PROVIDER_ID,
    BIOGRAPHY,
    IMAGE_ID,
    WEBSITE,
    BORN_IN,
    BIRTH_DATE,
    DEATH_DATE,
}

sealed class AuthorMetadata(
    name: String,
) : IdTable<UUID>(name) {
    final override val id = reference("author_id", AuthorTable, onDelete = ReferenceOption.CASCADE)
    final override val primaryKey = PrimaryKey(id)

    val name = text(AuthorField.NAME.column).nullable()
    val biography = text(AuthorField.BIOGRAPHY.column).nullable()
    val website = varchar(AuthorField.WEBSITE.column, 255).nullable()
    val birthDate = date(AuthorField.BIRTH_DATE.column).nullable()
    val bornIn = varchar(AuthorField.BORN_IN.column, 255).nullable()
    val deathDate = date(AuthorField.DEATH_DATE.column).nullable()
    val provider = varchar(AuthorField.PROVIDER.column, 255).nullable()
    val providerId = varchar(AuthorField.PROVIDER_ID.column, 255).nullable()
    val imageId = reference(AuthorField.IMAGE_ID.column, ImageTable, onDelete = ReferenceOption.SET_NULL).nullable()
    val claimed = json<Set<AuthorField>>("claimed").default(emptySet())
}

object AuthorFileMetadataTable : AuthorMetadata("author_file_metadata")

object AuthorAgentMetadataTable : AuthorMetadata("author_agent_metadata")

object AuthorUserMetadataTable : AuthorMetadata("author_user_metadata")

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
    override val claimed: Set<AuthorField> = emptySet(),
) : LayerRow<AuthorField>

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
        providerID = this[table.providerId],
        imageID = this[table.imageId]?.value,
        claimed = this[table.claimed],
    )

// Nothing outside of this file may write an author layer.
context(_: Transaction)
fun AuthorMetadata.write(row: AuthorMetadataRow) {
    val updated = update({ id eq row.author }) { write(it, row) }
    if (updated == 0) insert { write(it, row) }
    reconcileAuthor(row.author)
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
    stmt[providerId] = row.providerID
    stmt[imageId] = row.imageID
    stmt[claimed] = row.claimed
}
