package io.github.huiibuh.services.database

import api.exceptions.APINotFound
import io.github.huiibuh.db.tables.Book
import io.github.huiibuh.db.tables.TBooks
import io.github.huiibuh.db.tables.TTracks
import io.github.huiibuh.db.tables.Track
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object TrackService {
    fun get(uuid: UUID): Track = transaction {
        Track.findById(uuid) ?: throw APINotFound("Requested track was not found")
    }

    fun forBook(bookID: UUID, order: SortOrder = SortOrder.ASC) = transaction {
        rawForBook(bookID, order).map { it.toModel() }
    }

    fun rawForBook(bookID: UUID, order: SortOrder = SortOrder.ASC) = transaction {
        Track.find { TTracks.book eq bookID }.orderBy(TTracks.trackNr to order).toList()
    }

    fun forAuthor(uuid: UUID) = transaction {
        Book.find { TBooks.author eq uuid }.flatMap {
            Track.find { TTracks.book eq it.id }.toList()
        }
    }

    fun forSeries(uuid: UUID) = transaction {
        Book.find { TBooks.series eq uuid }
                .flatMap { Track.find { TTracks.book eq it.id }.toList() }
    }
}
