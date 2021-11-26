package io.github.huiibuh.services.database

import api.exceptions.APINotFound
import io.github.huiibuh.db.tables.TTracks
import io.github.huiibuh.db.tables.Track
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object TrackService {
    fun get(uuid: UUID): Track = transaction {
        Track.findById(uuid) ?: throw APINotFound("Requested track was not found")
    }

    fun forBook(bookID: UUID) = transaction {
        Track.find { TTracks.book eq bookID }.sortedBy { it.trackNr }.map { it.toModel() }
    }

    fun rawForBook(bookID: UUID) = transaction {
        Track.find { TTracks.book eq bookID }.sortedBy { it.trackNr }
    }

    fun forAuthor(uuid: UUID) = transaction {
        Track.find { TTracks.author eq uuid }.sortedBy { it.book.title }.sortedBy { it.trackNr }.toList()
    }
}
