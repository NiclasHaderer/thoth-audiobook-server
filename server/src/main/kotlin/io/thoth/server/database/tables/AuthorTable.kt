package io.thoth.server.database.tables

import io.thoth.models.Author
import org.jetbrains.exposed.v1.core.Coalesce
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.update
import java.time.LocalDate
import java.util.UUID

object AuthorTable : UUIDTable("Authors") {
    val name = text("name")
    val displayName = varchar("displayName", 255).nullable()
    val biography = text("biography").nullable()
    val website = varchar("website", 255).nullable()
    val birthDate = date("birthDate").nullable()
    val bornIn = varchar("bornIn", 255).nullable()
    val deathDate = date("deathDate").nullable()

    // Provider
    val provider = varchar("provider", 255).nullable()
    val providerID = varchar("providerID", 255).nullable()

    // Relations
    val imageID = reference("imageId", ImageTable, onDelete = ReferenceOption.SET_NULL).nullable()
    val library = reference("library", LibrariesTable, onDelete = ReferenceOption.CASCADE).index()

    val displayedName = Coalesce(displayName, name)
}

data class AuthorRow(
    val id: UUID,
    val name: String,
    val displayName: String?,
    val biography: String?,
    val website: String?,
    val birthDate: LocalDate?,
    val bornIn: String?,
    val deathDate: LocalDate?,
    val provider: String?,
    val providerID: String?,
    val imageID: UUID?,
    val library: UUID,
) {
    val displayedName: String
        get() = displayName ?: name

    fun toModel(): Author =
        Author(
            id = id,
            name = displayedName,
            biography = biography,
            provider = provider,
            birthDate = birthDate,
            bornIn = bornIn,
            deathDate = deathDate,
            imageID = imageID,
            website = website,
            providerID = providerID,
        )
}

fun ResultRow.toAuthorRow(): AuthorRow =
    AuthorRow(
        id = this[AuthorTable.id].value,
        name = this[AuthorTable.name],
        displayName = this[AuthorTable.displayName],
        biography = this[AuthorTable.biography],
        website = this[AuthorTable.website],
        birthDate = this[AuthorTable.birthDate],
        bornIn = this[AuthorTable.bornIn],
        deathDate = this[AuthorTable.deathDate],
        provider = this[AuthorTable.provider],
        providerID = this[AuthorTable.providerID],
        imageID = this[AuthorTable.imageID]?.value,
        library = this[AuthorTable.library].value,
    )

context(_: Transaction)
fun AuthorTable.insert(row: AuthorRow): UUID {
    insert { write(it, row) }
    return row.id
}

context(_: Transaction)
fun AuthorTable.update(row: AuthorRow) {
    update({ AuthorTable.id eq row.id }) { write(it, row) }
}

private fun write(
    stmt: UpdateBuilder<*>,
    row: AuthorRow,
) {
    stmt[AuthorTable.id] = row.id
    stmt[AuthorTable.name] = row.name
    stmt[AuthorTable.displayName] = row.displayName
    stmt[AuthorTable.biography] = row.biography
    stmt[AuthorTable.website] = row.website
    stmt[AuthorTable.birthDate] = row.birthDate
    stmt[AuthorTable.bornIn] = row.bornIn
    stmt[AuthorTable.deathDate] = row.deathDate
    stmt[AuthorTable.provider] = row.provider
    stmt[AuthorTable.providerID] = row.providerID
    stmt[AuthorTable.imageID] = row.imageID
    stmt[AuthorTable.library] = row.library
}
