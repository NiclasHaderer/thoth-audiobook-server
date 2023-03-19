package io.thoth.models

import io.thoth.common.serializion.kotlin.LocalDate_S
import io.thoth.common.serializion.kotlin.UUID_S

open class AuthorModel(
    val id: UUID_S,
    val name: String,
    val provider: String?,
    val providerID: String?,
    val biography: String?,
    val imageID: UUID_S?,
    val website: String?,
    val bornIn: String?,
    val birthDate: LocalDate_S?,
    val deathDate: LocalDate_S?
)

class DetailedAuthorModel(
    id: UUID_S,
    name: String,
    provider: String?,
    providerID: String?,
    biography: String?,
    imageID: UUID_S?,
    website: String?,
    bornIn: String?,
    birthDate: LocalDate_S?,
    deathDate: LocalDate_S?,
    val books: List<BookModel>,
    val series: List<SeriesModel>,
) :
    AuthorModel(
        id = id,
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
        fun fromModel(author: AuthorModel, books: List<BookModel>, series: List<SeriesModel>) =
            DetailedAuthorModel(
                id = author.id,
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
