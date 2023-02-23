package io.thoth.database.access

import io.thoth.common.extensions.findOne
import io.thoth.database.tables.Author
import io.thoth.database.tables.TAuthors
import io.thoth.database.tables.TBooks
import io.thoth.database.tables.TSeries
import io.thoth.models.AuthorModel
import io.thoth.models.DetailedAuthorModel
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.select
import java.util.*

fun Author.Companion.positionOf(authorId: UUID, order: SortOrder = SortOrder.ASC): Long? {
    val author = findById(authorId) ?: return null
    return TAuthors.select { TAuthors.name.lowerCase() less author.name.lowercase() }
        .orderBy(TAuthors.name.lowerCase() to order).count()
}

fun Author.Companion.getDetailedById(authorId: UUID, order: SortOrder = SortOrder.ASC): DetailedAuthorModel? {
    val author = findById(authorId) ?: return null

    return DetailedAuthorModel.fromModel(
        author = author.toModel(),
        books = author.books.orderBy(TBooks.releaseDate to order).map { it.toModel() },
        series = author.series.orderBy(TSeries.title.lowerCase() to order).map { it.toModel() },
    )
}

fun Author.Companion.getMultiple(limit: Int, offset: Long, order: SortOrder = SortOrder.ASC): List<AuthorModel> {
    return Author.all().limit(limit, offset).orderBy(TAuthors.name.lowerCase() to order).map { it.toModel() }
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
        providerID = providerID,
    )
}
