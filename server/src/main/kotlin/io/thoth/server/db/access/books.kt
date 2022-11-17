package io.thoth.server.db.access

import io.thoth.common.extensions.findOne
import io.thoth.common.extensions.get
import io.thoth.common.extensions.isTrue
import io.thoth.database.tables.Author
import io.thoth.database.tables.Book
import io.thoth.database.tables.TBooks
import io.thoth.database.tables.Track
import io.thoth.models.BookModel
import io.thoth.models.BookModelWithTracks
import io.thoth.models.NamedId
import io.thoth.models.TitledId
import io.thoth.server.config.ThothConfig
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.lowerCase
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
    return Book.find { TBooks.series eq seriesId }.orderBy(TBooks.title.lowerCase() to order).map { it.toModel() }
}

fun Book.Companion.findByName(bookTitle: String, author: Author): Book? {
    return Book.findOne { TBooks.title like bookTitle and (TBooks.author eq author.id.value) }
}


fun Book.Companion.count(): Long {
    return Book.all().count()
}

fun Book.Companion.getMultiple(limit: Int, offset: Long, order: SortOrder = SortOrder.ASC): List<BookModel> {
    return Book.all().limit(limit, offset * limit).orderBy(TBooks.title.lowerCase() to order).map { it.toModel() }
}


fun Book.Companion.fromAuthor(authorID: UUID, order: SortOrder = SortOrder.ASC): List<BookModel> {
    return find { TBooks.author eq authorID }.orderBy(TBooks.title.lowerCase() to order).map { it.toModel() }
}

fun Book.toModel(): BookModel {
    val preferMeta = get<ThothConfig>().preferEmbeddedMetadata

    return BookModel(
        id = id.value,
        title = preferMeta.isTrue(linkedTo?.title).otherwise(title),
        date = preferMeta.isTrue(linkedTo?.date).otherwise(date),
        language = preferMeta.isTrue(linkedTo?.language).otherwise(language),
        description = preferMeta.isTrue(linkedTo?.description).otherwise(description),
        author = preferMeta.isTrue(
            NamedId(
                id = author.id.value,
                // TODO multiple authors
                name = preferMeta.isTrue(linkedTo?.authors?.firstOrNull()?.name).otherwise(author.name)
            )
        ).otherwise(
            NamedId(
                id = author.id.value,
                name = author.name
            )
        ),
        narrator = preferMeta.isTrue(linkedTo?.narrator).otherwise(narrator),
        series = preferMeta.isTrue(
            // TODO check what happens if the meta book has a series and the read one not
            if (series != null) TitledId(
                id = series!!.id.value,
                // TODO multiple series
                title = preferMeta.isTrue(linkedTo?.series?.firstOrNull()?.title).otherwise(series!!.title)
            ) else null
        ).otherwise( if (series != null) TitledId(
            id = series!!.id.value,
            title = series!!.title
        ) else null),
        // TODO series index is not stored in an accessible way for the meta book
        seriesIndex = seriesIndex,
        cover = preferMeta.isTrue(linkedTo?.cover?.value).otherwise(cover?.value),
        updateTime = updateTime,
    )
}
