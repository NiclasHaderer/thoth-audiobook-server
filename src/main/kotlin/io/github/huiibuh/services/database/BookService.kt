package io.github.huiibuh.services.database

import api.exceptions.APINotFound
import io.github.huiibuh.db.tables.Book
import io.github.huiibuh.db.tables.TBooks
import io.github.huiibuh.models.BookModelWithTracks
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object BookService {
    fun getMultiple(limit: Int, offset: Long, order: SortOrder = SortOrder.ASC) = transaction {
        Book.all().limit(limit, offset * limit).orderBy(TBooks.title to order).map { it.toModel() }
    }

    fun get(uuid: UUID) = transaction {
        val book = Book.findById(uuid)?.toModel() ?: throw APINotFound("Could not find album")
        val tracks = TrackService.rawForBook(uuid)
        BookModelWithTracks.fromModel(book, tracks)
    }

    fun forSeries(seriesId: UUID, order: SortOrder = SortOrder.ASC) = transaction {
        Book.find { TBooks.series eq seriesId }.orderBy(TBooks.title to order).map { it.toModel() }
    }
}
