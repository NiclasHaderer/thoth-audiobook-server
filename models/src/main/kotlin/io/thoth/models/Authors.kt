package io.thoth.models

import io.thoth.common.serializion.kotlin.LocalDate_S
import io.thoth.common.serializion.kotlin.UUID_S
import kotlinx.serialization.Serializable
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

@Serializable
data class AuthorModel(
    override val id: UUID_S,
    override val name: String,
    override val provider: String?,
    override val biography: String?,
    override val imageID: UUID_S?,
    override val website: String?,
    override val bornIn: String?,
    override val birthDate: LocalDate_S?,
    override val deathDate: LocalDate_S?
) : IAuthorModel

@Serializable
data class DetailedAuthorModel(
    override val id: UUID_S,
    override val name: String,
    override val provider: String?,
    override val biography: String?,
    override val imageID: UUID_S?,
    override val website: String?,
    override val bornIn: String?,
    override val birthDate: LocalDate_S?,
    override val deathDate: LocalDate_S?,
    val books: List<IBookModel>,
    val series: List<ISeriesModel>,
) : IAuthorModel {
    companion object {
        fun fromModel(author: IAuthorModel, books: List<IBookModel>, series: List<ISeriesModel>) =
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
            )
    }
}
