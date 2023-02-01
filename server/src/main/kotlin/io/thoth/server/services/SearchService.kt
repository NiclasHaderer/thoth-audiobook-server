package io.thoth.server.services

import io.thoth.common.extensions.fuzzy
import io.thoth.common.extensions.saveTo
import io.thoth.database.access.toModel
import io.thoth.database.tables.Author
import io.thoth.database.tables.Book
import io.thoth.database.tables.Series
import io.thoth.models.SearchModel
import org.jetbrains.exposed.sql.transactions.transaction


object SearchService {
    fun everywhere(query: String, limit: Int = 5): SearchModel {
        return SearchModel(
            books = everywhereBook(query, limit),
            series = everywhereSeries(query, limit),
            authors = everywhereAuthor(query, limit),
        )
    }

    private fun everywhereAuthor(query: String, limit: Int) = transaction {
        val authors = Author.all().fuzzy(query) { listOfNotNull(it.name) }.saveTo(limit)
        val bookAuthors =
            Book.all().fuzzy(query) { listOfNotNull(it.title, it.narrator, it.series.joinToString(",") { it.title }) }
                .saveTo(limit).flatMap { it.authors }
        (authors + bookAuthors).distinctBy { it.id }.saveTo(limit).map { it.toModel() }
    }

    private fun everywhereSeries(query: String, limit: Int) = transaction {
        val series = Series.all().fuzzy(query) { listOfNotNull(it.title) }.saveTo(limit)
        val authorSeries =
            Book.all().fuzzy(query) { listOfNotNull(it.title, it.narrator, it.authors.joinToString(",") { it.name }) }
                .saveTo(limit).flatMap { it.series }
        (series + authorSeries).distinctBy { it.id }.saveTo(limit).map { it.toModel() }
    }

    private fun everywhereBook(query: String, limit: Int) = transaction {
        val books = Book.all().fuzzy(query) { listOfNotNull(it.title) }.saveTo(limit)
        val booksAndOther = Book.all().fuzzy(query) {
            listOfNotNull(
                it.authors.joinToString(", ") { it.name },
                it.series.joinToString(",") { it.title },
                it.narrator
            )
        }.saveTo(limit)
        (books + booksAndOther).distinctBy { it.id }.saveTo(limit).map { it.toModel() }
    }
}

