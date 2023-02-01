package io.thoth.database.access

import io.thoth.database.tables.*
import io.thoth.models.BookModel
import io.thoth.models.BookModelWithTracks
import io.thoth.models.NamedId
import io.thoth.models.TitledId
import org.jetbrains.exposed.sql.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

fun Book.Companion.getById(bookId: UUID): BookModel? {
    return findById(bookId)?.toModel() ?: return null
}

fun Book.Companion.getDetailedById(bookId: UUID, order: SortOrder = SortOrder.ASC): BookModelWithTracks? {
    val book = getById(bookId) ?: return null
    val tracks = Track.forBook(bookId)
    val sortPosition = Book.all().orderBy(TBooks.title.lowerCase() to order).count()
    return BookModelWithTracks.fromModel(book, tracks, sortPosition)
}

fun Book.Companion.forSeries(seriesId: UUID, order: SortOrder = SortOrder.ASC): List<BookModel> {
    return TSeriesBookMapping.select {
        TSeriesBookMapping.series eq seriesId
    }.map { it[TSeriesBookMapping.book] }.map { Book(it).toModel() }
}

fun Book.Companion.findByName(bookTitle: String, author: Author): Book? {
    val rawBook = TBooks
        .join(TAuthorBookMapping, JoinType.INNER, TBooks.id, TAuthorBookMapping.book)
        .join(TAuthors, JoinType.INNER, TAuthorBookMapping.author, TAuthors.id)
        .select { (TBooks.title like bookTitle) and (TAuthorBookMapping.author eq author.id) }.firstOrNull()
        ?: return null
    return Book.wrap(rawBook[TBooks.id], rawBook)
}


fun Book.Companion.count(): Long {
    return Book.all().count()
}

fun Book.Companion.getMultiple(limit: Int, offset: Long, order: SortOrder = SortOrder.ASC): List<BookModel> {
    return Book.all().limit(limit, offset * limit).orderBy(TBooks.title.lowerCase() to order).map { it.toModel() }
}


fun Book.Companion.fromAuthor(authorID: UUID, order: SortOrder = SortOrder.ASC): List<BookModel> {
    val bookIDs = TAuthorBookMapping.select { TAuthorBookMapping.author eq authorID }
        .map { it[TAuthorBookMapping.book] }

    return Book.find { TBooks.id inList bookIDs }.orderBy(TBooks.title.lowerCase() to order).map { it.toModel() }
}

fun Book.toModel(): BookModel {
    return BookModel(
        id = id.value,
        title = title,
        // TODO check if this is correct
        date = LocalDate.now(),
        language = language,
        description = description,
        authors = authors.map { NamedId(it.name, it.id.value) },
        narrator = narrator,
        series = series.map { TitledId(it.title, it.id.value) },
        // TODO series index is not stored in an accessible way for the meta book
        seriesIndex = -1f,
        cover = coverID?.value,
        // TODO check if this is correct
        updateTime = LocalDateTime.now(),
    )
}
