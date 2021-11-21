package io.github.huiibuh.services.database

import api.exceptions.APINotFound
import io.github.huiibuh.db.tables.Book
import io.github.huiibuh.db.tables.TBooks
import io.github.huiibuh.db.tables.Series
import io.github.huiibuh.models.SeriesModelWithBooks
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object SeriesService {

    fun getMultiple(limit: Int, offset: Long) = transaction {
        Series.all().limit(limit, offset).map {
            it.toModel()
        }
    }

    fun get(id: UUID) = transaction {
        val series = Series.findById(id)?.toModel() ?: throw APINotFound("Could not find series")
        val books = Book.find { TBooks.series eq id }.map { it.toModel() }
        SeriesModelWithBooks.fromModel(series, books)
    }
}
