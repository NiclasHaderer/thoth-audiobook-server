package io.github.huiibuh.models

import java.util.*

interface IAuthorModel {
    val id: UUID
    val name: String
    val biography: String?
    val providerID: ProviderIDModel?
    val image: UUID?
}

class AuthorModel(
    override val id: UUID,
    override val name: String,
    override val biography: String?,
    override val providerID: ProviderIDModel?,
    override val image: UUID?,
) : IAuthorModel

class AuthorModelWithBooks(
    override val id: UUID,
    override val name: String,
    override val biography: String?,
    override val providerID: ProviderIDModel?,
    override val image: UUID?,
    val position: Int,
    val books: List<IBookModel>,
) : IAuthorModel {
    companion object {
        fun fromModel(author: IAuthorModel, books: List<IBookModel>, position: Int) = AuthorModelWithBooks(
            id = author.id,
            name = author.name,
            biography = author.biography,
            position = position,
            providerID = author.providerID,
            image = author.image,
            books = books
        )
    }
}
