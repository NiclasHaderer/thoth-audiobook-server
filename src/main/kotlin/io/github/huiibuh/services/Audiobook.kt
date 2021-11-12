package io.github.huiibuh.services

import io.github.huiibuh.db.models.Album
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object AudiobookService {
    fun getBook(id: UUID): Album {
        val album = transaction {
            Album.findById(id)
        }
        if (album == null) {
            // TODO not found exception
            throw Exception("huii")
        }
        return album
    }
}
