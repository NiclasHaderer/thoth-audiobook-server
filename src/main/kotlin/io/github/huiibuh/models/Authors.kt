package io.github.huiibuh.models

import io.github.huiibuh.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

interface IAuthorModel {
    val id: UUID
    val name: String
    val biography: String?
    val asin: String?
    val image: UUID?
}

@Serializable
data class AuthorModel(
    @Serializable(UUIDSerializer::class) override val id: UUID,
    override val name: String,
    override val biography: String?,
    override val asin: String?,
    @Serializable(UUIDSerializer::class) override val image: UUID?,
) : IAuthorModel

data class AuthorModelWithBooks(
    @Serializable(UUIDSerializer::class) override val id: UUID,
    override val name: String,
    override val biography: String?,
    override val asin: String?,
    @Serializable(UUIDSerializer::class) override val image: UUID?,
    val position: Int,
    val books: List<BookModel>,
) : IAuthorModel {
    companion object {
        fun fromModel(author: AuthorModel, books: List<BookModel>, position: Int) = AuthorModelWithBooks(
            id = author.id,
            name = author.name,
            biography = author.biography,
            position = position,
            asin = author.asin,
            image = author.image,
            books = books
        )
    }
}
