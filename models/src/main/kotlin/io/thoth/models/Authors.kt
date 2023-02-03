package io.thoth.models

import java.time.LocalDate
import java.util.*

interface IAuthorModel {
    val id: UUID
    val name: String
    val provider: String?
    val biography: String?
    val imageID: UUID?
    val website: String?
    val bornIn: String?
    val birthDate: LocalDate?
    val deathDate: LocalDate?
}

class AuthorModel(
    override val id: UUID,
    override val name: String,
    override val provider: String?,
    override val biography: String?,
    override val imageID: UUID?,
    override val website: String?,
    override val bornIn: String?,
    override val birthDate: LocalDate?,
    override val deathDate: LocalDate?
) : IAuthorModel

class AuthorModelWithBooks(
    override val id: UUID,
    override val name: String,
    override val provider: String?,
    override val biography: String?,
    override val imageID: UUID?,
    override val website: String?,
    override val bornIn: String?,
    override val birthDate: LocalDate?,
    override val deathDate: LocalDate?,
    val books: List<IBookModel>,
    val series: List<ISeriesModel>
) : IAuthorModel {
    companion object {
        fun fromModel(author: IAuthorModel, books: List<IBookModel>, series: List<ISeriesModel>) = AuthorModelWithBooks(
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
            provider = author.provider
        )
    }
}
