package io.github.huiibuh.services

import io.github.huiibuh.db.tables.Author
import io.github.huiibuh.db.tables.Book
import io.github.huiibuh.db.tables.Series
import io.github.huiibuh.extensions.fuzzy
import io.github.huiibuh.extensions.saveTo
import io.github.huiibuh.models.SearchModel
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
        val authors = Author.all().fuzzy(query) { listOfNotNull(it.name, it.providerID?.itemID) }.saveTo(limit)
        val bookAuthors =
            Book.all().fuzzy(query) { listOfNotNull(it.title, it.narrator, it.series?.title) }.saveTo(limit)
                .map { it.author }
        (authors + bookAuthors).distinctBy { it.id }.saveTo(limit).map { it.toModel() }
    }

    private fun everywhereSeries(query: String, limit: Int) = transaction {
        val series = Series.all().fuzzy(query) { listOfNotNull(it.title, it.providerID?.itemID) }.saveTo(limit)
        val authorSeries = Book.all().fuzzy(query) { listOfNotNull(it.title, it.narrator, it.author.name) }
            .saveTo(limit).mapNotNull { it.series }
        (series + authorSeries).distinctBy { it.id }.saveTo(limit).map { it.toModel() }
    }

    private fun everywhereBook(query: String, limit: Int) = transaction {
        val books = Book.all().fuzzy(query) { listOfNotNull(it.title, it.providerID?.itemID) }
            .saveTo(limit)
        val booksAndOther = Book.all().fuzzy(query) { listOfNotNull(it.author.name, it.series?.title, it.narrator) }
            .saveTo(limit)
        (books + booksAndOther).distinctBy { it.id }.saveTo(limit).map { it.toModel() }
    }
}

