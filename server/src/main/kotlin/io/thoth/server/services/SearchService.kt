package io.thoth.server.services

import io.thoth.common.extensions.fuzzy
import io.thoth.common.extensions.saveTo
import io.thoth.database.tables.Author
import io.thoth.database.tables.Book
import io.thoth.database.tables.Series
import io.thoth.models.SearchModel
import io.thoth.server.db.access.toModel
import org.jetbrains.exposed.sql.transactions.transaction


object SearchService {
    fun everywhere(query: String, limit: Int = 5): SearchModel {
        return SearchModel(
            books = everywhereBook(query, limit),
            series = everywhereSeries(query, limit),
            authors = everywhereAuthor(query, limit),
        )
    }
    // List<io.thoth.models.datastructures.BookModel>
    // List<io.thoth.models.BookModel>

    private fun everywhereAuthor(query: String, limit: Int) = transaction {
        val authors = Author.all().fuzzy(query) { listOfNotNull(it.name) }.saveTo(limit)
        val bookAuthors =
            Book.all().fuzzy(query) { listOfNotNull(it.title, it.narrator, it.series?.title) }.saveTo(limit)
                .map { it.author }
        (authors + bookAuthors).distinctBy { it.id }.saveTo(limit).map { it.toModel() }
    }

    private fun everywhereSeries(query: String, limit: Int) = transaction {
        val series = Series.all().fuzzy(query) { listOfNotNull(it.title) }.saveTo(limit)
        val authorSeries = Book.all().fuzzy(query) { listOfNotNull(it.title, it.narrator, it.author.name) }
            .saveTo(limit).mapNotNull { it.series }
        (series + authorSeries).distinctBy { it.id }.saveTo(limit).map { it.toModel() }
    }

    private fun everywhereBook(query: String, limit: Int) = transaction {
        val books = Book.all().fuzzy(query) { listOfNotNull(it.title) }
            .saveTo(limit)
        val booksAndOther = Book.all().fuzzy(query) { listOfNotNull(it.author.name, it.series?.title, it.narrator) }
            .saveTo(limit)
        (books + booksAndOther).distinctBy { it.id }.saveTo(limit).map { it.toModel() }
    }
}

