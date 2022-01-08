package io.github.huiibuh.services.database

import io.github.huiibuh.api.exceptions.APINotFound
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

    fun get(uuid: UUID, order: SortOrder = SortOrder.ASC) = transaction {
        val book = Book.findById(uuid)?.toModel() ?: throw APINotFound("Could not find album")
        val tracks = TrackService.rawForBook(uuid)
        val index = Book.all().orderBy(TBooks.title to order).indexOfFirst { it.id.value == uuid }
        BookModelWithTracks.fromModel(book, tracks, index)
    }

    fun forSeries(seriesId: UUID, order: SortOrder = SortOrder.ASC) = transaction {
        Book.find { TBooks.series eq seriesId }.orderBy(TBooks.title to order).map { it.toModel() }
    }
}
