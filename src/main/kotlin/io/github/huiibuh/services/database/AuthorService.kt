package io.github.huiibuh.services.database

import api.exceptions.APINotFound
import io.github.huiibuh.db.tables.Author
import io.github.huiibuh.db.tables.Book
import io.github.huiibuh.db.tables.TAuthors
import io.github.huiibuh.db.tables.TBooks
import io.github.huiibuh.models.AuthorModelWithBooks
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object AuthorService {
    fun get(uuid: UUID, order: SortOrder = SortOrder.ASC) = transaction {
        val author = Author.findById(uuid)?.toModel() ?: throw APINotFound("Could not find author")
        val books = Book.find { TBooks.author eq uuid }.orderBy(TBooks.year to order).map { it.toModel() }
        AuthorModelWithBooks.fromModel(author, books)
    }

    fun getMultiple(limit: Int, offset: Long, order: SortOrder = SortOrder.ASC) = transaction {
        Author.all().limit(limit, offset * limit).orderBy(TAuthors.name to order).map { it.toModel() }
    }
}
