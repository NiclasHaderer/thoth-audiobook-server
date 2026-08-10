package io.thoth.server.database.views

import io.thoth.models.Author
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
        id = this[AuthorMetadataView.id],
        library = this[AuthorMetadataView.library],
        name = this[AuthorMetadataView.name],
        biography = this[AuthorMetadataView.biography],
        website = this[AuthorMetadataView.website],
        birthDate = this[AuthorMetadataView.birthDate],
        bornIn = this[AuthorMetadataView.bornIn],
        deathDate = this[AuthorMetadataView.deathDate],
        provider = this[AuthorMetadataView.provider],
        providerID = this[AuthorMetadataView.providerID],
        imageID = this[AuthorMetadataView.imageId],
    )
