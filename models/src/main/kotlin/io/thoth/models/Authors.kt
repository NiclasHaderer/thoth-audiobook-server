package io.thoth.models

import java.time.LocalDate
import java.util.*

open class AuthorModel(
    val id: UUID,
    val name: String,
    val provider: String?,
    val providerID: String?,
    val biography: String?,
    val imageID: UUID?,
    val website: String?,
    val bornIn: String?,
    val birthDate: LocalDate?,
    val deathDate: LocalDate?
)

class DetailedAuthorModel(
    id: UUID,
    name: String,
    provider: String?,
    providerID: String?,
    biography: String?,
    imageID: UUID?,
    website: String?,
    bornIn: String?,
    birthDate: LocalDate?,
    deathDate: LocalDate?,
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
