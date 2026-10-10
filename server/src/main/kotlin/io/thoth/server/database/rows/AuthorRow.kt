package io.thoth.server.database.rows

import io.thoth.models.Author
import io.thoth.server.database.tables.AuthorField
import io.thoth.server.database.tables.AuthorTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.update
import java.time.LocalDate
import java.util.UUID

data class AuthorRow(
    val id: UUID,
    val library: UUID,
    val name: String,
    val taggedName: String?,
    val biography: String?,
    val website: String?,
    val birthDate: LocalDate?,
    val bornIn: String?,
    val deathDate: LocalDate?,
    val provider: String?,
    val providerID: String?,
    val imageID: UUID?,
    val locked: Set<AuthorField>,
) {
    fun toModel(): Author =
        Author(
            id = id,
            libraryId = library,
            name = name,
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
        library = this[AuthorTable.library].value,
        name = this[AuthorTable.name],
        taggedName = this[AuthorTable.taggedName],
        biography = this[AuthorTable.biography],
        website = this[AuthorTable.website],
        birthDate = this[AuthorTable.birthDate],
        bornIn = this[AuthorTable.bornIn],
        deathDate = this[AuthorTable.deathDate],
        provider = this[AuthorTable.provider],
        providerID = this[AuthorTable.providerId],
        imageID = this[AuthorTable.imageId]?.value,
        locked = this[AuthorTable.locked],
    )

context(_: Transaction)
fun AuthorTable.update(row: AuthorRow) {
    update({ id eq row.id }) {
        it[name] = row.name
        it[taggedName] = row.taggedName
        it[biography] = row.biography
        it[website] = row.website
        it[birthDate] = row.birthDate
        it[bornIn] = row.bornIn
        it[deathDate] = row.deathDate
        it[provider] = row.provider
        it[providerId] = row.providerID
        it[imageId] = row.imageID
        it[locked] = row.locked
    }
}
