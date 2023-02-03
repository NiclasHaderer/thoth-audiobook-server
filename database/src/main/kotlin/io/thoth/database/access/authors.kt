package io.thoth.database.access

import io.thoth.common.extensions.findOne
import io.thoth.database.tables.Author
import io.thoth.database.tables.TAuthors
import io.thoth.database.tables.TBooks
import io.thoth.database.tables.TSeries
import io.thoth.models.AuthorModel
import io.thoth.models.AuthorModelWithBooks
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.lowerCase
import java.util.*

fun Author.Companion.getById(authorId: UUID): AuthorModel? {
    return findById(authorId)?.toModel() ?: return null
}

fun Author.Companion.getDetailedById(authorId: UUID, order: SortOrder = SortOrder.ASC): AuthorModelWithBooks? {
    val author = findById(authorId) ?: return null


    return AuthorModelWithBooks.fromModel(
        author = author.toModel(),
        books = author.books.orderBy(TBooks.published to order).map { it.toModel() },
        series = author.series.orderBy(TSeries.title.lowerCase() to order).map { it.toModel() }
    )
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
    return AuthorModel(
        id = id.value,
        name = name,
        biography = biography,
        provider = provider,
        birthDate = birthDate,
        bornIn = bornIn,
        deathDate = deathDate,
        imageID = imageID?.value,
        website = website,
    )
}
