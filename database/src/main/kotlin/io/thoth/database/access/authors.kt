package io.thoth.database.access

import io.thoth.database.tables.Author
import io.thoth.models.AuthorModel

fun Author.toModel(): AuthorModel {
    return AuthorModel(
        id = id.value,
        name = name,
        biography = biography,
        provider = provider,
        birthDate = birthDate,
        bornIn = bornIn,
        deathDate = deathDate,
        imageID = imageID?.value,
        website = website,
        providerID = providerID,
    )
}
