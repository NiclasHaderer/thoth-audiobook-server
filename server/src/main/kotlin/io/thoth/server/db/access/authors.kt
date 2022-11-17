package io.thoth.server.db.access

import io.thoth.common.extensions.findOne
import io.thoth.common.extensions.get
import io.thoth.common.extensions.isTrue
import io.thoth.database.tables.Author
import io.thoth.database.tables.Book
import io.thoth.database.tables.TAuthors
import io.thoth.models.AuthorModel
import io.thoth.models.AuthorModelWithBooks
import io.thoth.server.config.ThothConfig
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.lowerCase
import java.util.*

fun Author.Companion.getById(authorId: UUID): AuthorModel? {
    return findById(authorId)?.toModel() ?: return null
}

fun Author.Companion.getDetailedById(authorId: UUID, order: SortOrder = SortOrder.ASC): AuthorModelWithBooks? {
    val author = getById(authorId) ?: return null
    val books = Book.fromAuthor(authorId, order)
    val index = Author.all().orderBy(TAuthors.name.lowerCase() to order).takeWhile {
        it.id.value != authorId
    }.count()


    return AuthorModelWithBooks.fromModel(author, books, index)
}

fun Author.Companion.getMultiple(limit: Int, offset: Long, order: SortOrder = SortOrder.ASC): List<AuthorModel> {
    return Author.all().limit(limit, offset * limit).orderBy(TAuthors.name.lowerCase() to order).map { it.toModel() }
}


fun Author.Companion.count(): Long {
    return Author.all().count()
}

fun Author.Companion.findByName(authorName: String): Author? {
    return findOne { TAuthors.name like authorName }
}

fun Author.toModel(): AuthorModel {
    val preferMeta = get<ThothConfig>().preferEmbeddedMetadata

    return AuthorModel(
        id = id.value,
        name = preferMeta.isTrue(linkedTo?.name).otherwise(name),
        biography = preferMeta.isTrue(linkedTo?.biography).otherwise(biography),
        image = preferMeta.isTrue(linkedTo?.imageId?.value).otherwise(imageId?.value),
    )
}
