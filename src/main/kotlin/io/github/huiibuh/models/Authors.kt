package io.github.huiibuh.models

import io.github.huiibuh.serializers.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class AuthorModel(
    @Serializable(UUIDSerializer::class) val id: UUID,
    val name: String,
    val biography: String?,
    val asin: String?,
    @Serializable(UUIDSerializer::class) val image: UUID?,
)

data class AuthorModelWithBooks(
    @Serializable(UUIDSerializer::class) val id: UUID,
    val name: String,
    val biography: String?,
    val asin: String?,
    val books: List<BookModel>,
    @Serializable(UUIDSerializer::class) val image: UUID?,
) {
    companion object {
        fun fromModel(author: AuthorModel, books: List<BookModel>) = AuthorModelWithBooks(
            id = author.id,
            name = author.name,
            biography = author.biography,
            asin = author.asin,
            image = author.image,
            books = books
        )
    }
}
