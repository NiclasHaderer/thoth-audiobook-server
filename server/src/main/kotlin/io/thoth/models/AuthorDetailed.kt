package io.thoth.models

import java.time.LocalDate
import java.util.UUID

class AuthorDetailed(
    id: UUID,
    libraryId: UUID,
    name: String,
    provider: String?,
    providerID: String?,
    biography: String?,
    imageID: UUID?,
    website: String?,
    bornIn: String?,
    birthDate: LocalDate?,
    deathDate: LocalDate?,
    val books: List<Book>,
    val series: List<Series>,
) : Author(
        id = id,
        libraryId = libraryId,
        name = name,
        provider = provider,
        providerID = providerID,
        biography = biography,
        imageID = imageID,
        website = website,
        bornIn = bornIn,
        birthDate = birthDate,
        deathDate = deathDate,
    ) {
    companion object {
        fun fromModel(
            author: Author,
            books: List<Book>,
            series: List<Series>,
        ) = AuthorDetailed(
            id = author.id,
            libraryId = author.libraryId,
            name = author.name,
            biography = author.biography,
            imageID = author.imageID,
            website = author.website,
            bornIn = author.bornIn,
            birthDate = author.birthDate,
            deathDate = author.deathDate,
            books = books,
            series = series,
            provider = author.provider,
            providerID = author.providerID,
        )
    }
}
