package io.github.huiibuh.services.database

import io.github.huiibuh.db.tables.Author
import io.github.huiibuh.db.tables.Book
import io.github.huiibuh.db.tables.Series
import io.github.huiibuh.models.SearchModel
import io.github.huiibuh.utils.fuzzy
import io.github.huiibuh.utils.to
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction


@Serializable
object SearchService {
    fun everywhere(query: String, limit: Int = 5): SearchModel {
        return SearchModel(
            books = everywhereBook(query, limit),
            series = everywhereSeries(query, limit),
            authors = everywhereAuthor(query, limit),
        )
    }

    private fun everywhereAuthor(query: String, limit: Int) = transaction {
        val authors = Author.all().fuzzy(query) { listOfNotNull(it.name, it.asin) }.to(limit)
        val bookAuthors = Book.all().fuzzy(query) { listOfNotNull(it.title, it.narrator, it.series?.title) }
                .to(limit).map { it.author }
        (authors + bookAuthors).distinctBy { it.id }.to(limit).map { it.toModel() }
    }

    private fun everywhereSeries(query: String, limit: Int) = transaction {
        val series = Series.all().fuzzy(query) { listOfNotNull(it.title, it.asin) }.to(limit)
        val authorSeries = Book.all().fuzzy(query) { listOfNotNull(it.title, it.narrator, it.author.name) }
                .to(limit).mapNotNull { it.series }
        (series + authorSeries).distinctBy { it.id }.to(limit).map { it.toModel() }
    }

    private fun everywhereBook(query: String, limit: Int) = transaction {
        val books = Book.all().fuzzy(query) { listOfNotNull(it.title, it.asin) }
                .to(limit)
        val booksAndOther = Book.all().fuzzy(query) { listOfNotNull(it.author.name, it.series?.title, it.narrator) }
                .to(limit)
        (books + booksAndOther).distinctBy { it.id }.to(limit).map { it.toModel() }
    }
}

