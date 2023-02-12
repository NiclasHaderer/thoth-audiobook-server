package io.thoth.models

import io.thoth.common.serializion.kotlin.LocalDateSerializer
import io.thoth.common.serializion.kotlin.UUIDSerializer
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
    @Serializable(UUIDSerializer::class) override val id: UUID,
    override val name: String,
    override val provider: String?,
    override val biography: String?,
    @Serializable(UUIDSerializer::class) override val imageID: UUID?,
    override val website: String?,
    override val bornIn: String?,
    @Serializable(LocalDateSerializer::class) override val birthDate: LocalDate?,
    @Serializable(LocalDateSerializer::class) override val deathDate: LocalDate?
) : IAuthorModel

@Serializable
data class AuthorModelWithBooks(
    @Serializable(UUIDSerializer::class) override val id: UUID,
    override val name: String,
    override val provider: String?,
    override val biography: String?,
    @Serializable(UUIDSerializer::class) override val imageID: UUID?,
    override val website: String?,
    override val bornIn: String?,
    @Serializable(LocalDateSerializer::class) override val birthDate: LocalDate?,
    @Serializable(LocalDateSerializer::class) override val deathDate: LocalDate?,
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
