package io.thoth.server.repositories

import io.thoth.models.SearchModel
import io.thoth.server.common.extensions.fuzzy
import io.thoth.server.common.extensions.saveTo
import io.thoth.server.database.access.toModel
import io.thoth.server.database.tables.Author
import io.thoth.server.database.tables.Book
import io.thoth.server.database.tables.Series
import io.thoth.server.database.tables.TAuthors
import io.thoth.server.database.tables.TBooks
import io.thoth.server.database.tables.TSeries
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

object SearchRepository {
    fun everywhere(
        query: String,
        libsToSearch: List<UUID>,
        limit: Int = 5,
    ): SearchModel =
        SearchModel(
            books = everywhereBook(query, libsToSearch, limit),
            series = everywhereSeries(query, libsToSearch, limit),
            authors = everywhereAuthor(query, libsToSearch, limit),
        )

    private fun everywhereAuthor(
        query: String,
        libsToSearch: List<UUID>,
        limit: Int,
    ) = transaction {
        val authors =
            Author.find { TAuthors.library inList libsToSearch }.fuzzy(query) { listOfNotNull(it.name) }.saveTo(limit)
        val bookAuthors =
            Book
                .find { TBooks.library inList libsToSearch }
                .fuzzy(query) { listOfNotNull(it.title, it.narrator, it.series.joinToString(",") { it.title }) }
                .saveTo(limit)
                .flatMap { it.authors }
        (authors + bookAuthors).distinctBy { it.id }.saveTo(limit).map { it.toModel() }
    }

    private fun everywhereSeries(
        query: String,
        libsToSearch: List<UUID>,
        limit: Int,
    ) = transaction {
        val series =
            Series.find { TSeries.library inList libsToSearch }.fuzzy(query) { listOfNotNull(it.title) }.saveTo(limit)
        val authorSeries =
            Book
                .find { TBooks.library inList libsToSearch }
                .fuzzy(query) { listOfNotNull(it.title, it.narrator, it.authors.joinToString(",") { it.name }) }
                .saveTo(limit)
                .flatMap { it.series }
        (series + authorSeries).distinctBy { it.id }.saveTo(limit).map { it.toModel() }
    }

    private fun everywhereBook(
        query: String,
        libsToSearch: List<UUID>,
        limit: Int,
    ) = transaction {
        val books =
            Book.find { TBooks.library inList libsToSearch }.fuzzy(query) { listOfNotNull(it.title) }.saveTo(limit)
        val booksAndOther =
            Book
                .find { TBooks.library inList libsToSearch }
                .fuzzy(query) {
                    listOfNotNull(
                        it.authors.joinToString(", ") { it.name },
                        it.series.joinToString(",") { it.title },
                        it.narrator,
                    )
                }.saveTo(limit)
        (books + booksAndOther).distinctBy { it.id }.saveTo(limit).map { it.toModel() }
    }
}
