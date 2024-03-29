package io.thoth.server.database.access

import io.thoth.models.AuthorModel
import io.thoth.models.TitledId
import io.thoth.server.database.tables.Author

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
        library = TitledId(library.id.value, library.name),
    )
}
