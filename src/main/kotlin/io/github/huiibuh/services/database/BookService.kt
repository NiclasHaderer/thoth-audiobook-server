package io.github.huiibuh.services.database

import api.exceptions.APINotFound
import io.github.huiibuh.db.tables.Book
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object BookService {
    fun getMultiple(limit: Int, offset: Long) = transaction {
        Book.all().limit(limit, offset).map { it.toModel() }
    }

    fun get(uuid: UUID) = transaction {
        Book.findById(uuid)?.toModel() ?: throw APINotFound("Could not find album")
    }
}
