package io.github.huiibuh.services.database

import api.exceptions.APINotFound
import io.github.huiibuh.db.tables.Author
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object AuthorService {
    fun get(uuid: UUID) = transaction {
        Author.findById(uuid)?.toModel() ?: throw APINotFound("Could not find author")
    }

    fun getMultiple(limit: Int, offset: Long) = transaction {
        Author.all().limit(limit, offset).map { it.toModel() }
    }
}
