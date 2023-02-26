package io.thoth.database.access

import io.thoth.database.tables.*
import io.thoth.models.BookModel
import io.thoth.models.DetailedBookModel
import io.thoth.models.NamedId
import io.thoth.models.TitledId
import org.jetbrains.exposed.sql.*
import java.util.*

fun Book.Companion.getDetailedById(bookId: UUID): DetailedBookModel? {
    val book = findById(bookId) ?: return null
    val tracks = Track.forBook(bookId)
    return DetailedBookModel.fromModel(
        book.toModel(),
        tracks,
    )
}

fun Book.Companion.positionOf(bookId: UUID, order: SortOrder = SortOrder.ASC): Long? {
    val book = findById(bookId) ?: return null
    return TBooks.select { TBooks.title.lowerCase() less book.title.lowercase() }
        .orderBy(TBooks.title.lowerCase() to order).count()
}

fun Book.Companion.forSeries(seriesId: UUID, order: SortOrder = SortOrder.ASC): List<BookModel> {
    return TSeriesBookMapping.join(TBooks, JoinType.INNER, TSeriesBookMapping.book, TBooks.id)
        .join(TSeries, JoinType.INNER, TSeriesBookMapping.series, TSeries.id)
        .select { TSeriesBookMapping.series eq seriesId }.orderBy(TSeriesBookMapping.seriesIndex to order)
        .map { Book.wrap(it[TBooks.id], it).toModel() }
}

fun Book.Companion.findByName(bookTitle: String, author: Author): Book? {
    val rawBook = TBooks.join(TAuthorBookMapping, JoinType.INNER, TBooks.id, TAuthorBookMapping.book)
        .join(TAuthors, JoinType.INNER, TAuthorBookMapping.author, TAuthors.id)
        .select { (TBooks.title like bookTitle) and (TAuthorBookMapping.author eq author.id) }.firstOrNull()
        ?: return null
    return Book.wrap(rawBook[TBooks.id], rawBook)
}

fun Book.Companion.getMultiple(limit: Int, offset: Long, order: SortOrder = SortOrder.ASC): List<BookModel> {
    return Book.all().limit(limit, offset).orderBy(TBooks.title.lowerCase() to order).map { it.toModel() }
}


fun Book.Companion.fromAuthor(authorID: UUID, order: SortOrder = SortOrder.ASC): List<BookModel> {
    val bookIDs =
        TAuthorBookMapping.select { TAuthorBookMapping.author eq authorID }.map { it[TAuthorBookMapping.book] }

    return Book.find { TBooks.id inList bookIDs }.orderBy(TBooks.title.lowerCase() to order).map { it.toModel() }
}

fun Book.toModel(order: SortOrder = SortOrder.ASC): BookModel {
    return BookModel(
        id = id.value,
        title = title,
        description = description,
        providerID = providerID,
        provider = provider,
        providerRating = providerRating,
        coverID = coverID?.value,
        releaseDate = releaseDate,
        narrator = narrator,
        isbn = isbn,
        language = language,
        publisher = publisher,
        authors = authors.sortedBy { it.name.lowercase() }.map { NamedId(it.id.value, it.name) }.let {
            if (order == SortOrder.DESC) it.reversed() else it
        },
        series = series.sortedBy { it.title.lowercase() }.map { TitledId(it.id.value, it.title) }.let {
            if (order == SortOrder.DESC) it.reversed() else it
        },
    )
}
