package io.thoth.server.database.rows

import io.thoth.models.Author
import io.thoth.server.database.tables.AuthorTable
import org.jetbrains.exposed.v1.core.ResultRow
import java.time.LocalDate
import java.util.UUID

data class AuthorRow(
    val id: UUID,
    val library: UUID,
    val name: String,
    val biography: String?,
    val website: String?,
    val birthDate: LocalDate?,
    val bornIn: String?,
    val deathDate: LocalDate?,
    val provider: String?,
    val providerID: String?,
    val imageID: UUID?,
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
        biography = this[AuthorTable.biography],
        website = this[AuthorTable.website],
        birthDate = this[AuthorTable.birthDate],
        bornIn = this[AuthorTable.bornIn],
        deathDate = this[AuthorTable.deathDate],
        provider = this[AuthorTable.provider],
        providerID = this[AuthorTable.providerID],
        imageID = this[AuthorTable.imageID]?.value,
    )
