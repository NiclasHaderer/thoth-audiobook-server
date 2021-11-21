package io.github.huiibuh.services.database

import api.exceptions.APINotFound
import io.github.huiibuh.db.tables.Book
import io.github.huiibuh.db.tables.TTracks
import io.github.huiibuh.db.tables.Track
import io.github.huiibuh.models.BookWithTracks
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object BookService {
    fun getMultiple(limit: Int, offset: Long) = transaction {
        Book.all().limit(limit, offset).map { it.toModel() }
    }

    fun get(uuid: UUID) = transaction {
        val book = Book.findById(uuid)?.toModel() ?: throw APINotFound("Could not find album")
        val tracks = Track.find { TTracks.book eq uuid }.map { it.toModel() }
        BookWithTracks.fromBookModel(book, tracks)
    }
}
