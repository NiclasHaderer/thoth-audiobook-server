package io.thoth.server.repositories

import io.thoth.models.Author
import io.thoth.models.Book
import io.thoth.models.LibrarySearchResult
import io.thoth.models.Series
import io.thoth.server.common.extensions.fuzzy
import io.thoth.server.database.tables.AuthorBookTable
import io.thoth.server.database.tables.AuthorRow
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.BookRow
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.SeriesBookTable
import io.thoth.server.database.tables.SeriesRow
import io.thoth.server.database.tables.SeriesTable
import io.thoth.server.database.tables.booksToModels
import io.thoth.server.database.tables.seriesToModels
import io.thoth.server.database.tables.toAuthorRow
import io.thoth.server.database.tables.toBookRow
import io.thoth.server.database.tables.toSeriesRow
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

// TODO we can improve the performance of this by a lot...
object SearchRepository {
    fun everywhere(
        query: String,
        libsToSearch: List<UUID>,
        limit: Int = 5,
    ): LibrarySearchResult =
        transaction {
            val books = BooksTable.selectAll().where { BooksTable.library inList libsToSearch }.map { it.toBookRow() }
            val authors =
                AuthorTable.selectAll().where { AuthorTable.library inList libsToSearch }.map { it.toAuthorRow() }
            val series =
                SeriesTable.selectAll().where { SeriesTable.library inList libsToSearch }.map { it.toSeriesRow() }

            val bookIds = books.map { it.id }
            val authorsById = authors.associateBy { it.id }
            val seriesById = series.associateBy { it.id }
            val authorsByBook =
                AuthorBookTable
                    .selectAll()
                    .where { AuthorBookTable.book inList bookIds }
                    .groupBy({ it[AuthorBookTable.book].value }) { authorsById[it[AuthorBookTable.authors].value] }
                    .mapValues { (_, rows) -> rows.filterNotNull() }
            val seriesByBook =
                SeriesBookTable
                    .selectAll()
                    .where { SeriesBookTable.book inList bookIds }
                    .groupBy({ it[SeriesBookTable.book].value }) { seriesById[it[SeriesBookTable.series].value] }
                    .mapValues { (_, rows) -> rows.filterNotNull() }

            val index = SearchIndex(books, authors, series, authorsByBook, seriesByBook)
            LibrarySearchResult(
                books = everywhereBook(query, index, limit),
                series = everywhereSeries(query, index, limit),
                authors = everywhereAuthor(query, index, limit),
            )
        }

    private class SearchIndex(
        val books: List<BookRow>,
        val authors: List<AuthorRow>,
        val series: List<SeriesRow>,
        val authorsByBook: Map<UUID, List<AuthorRow>>,
        val seriesByBook: Map<UUID, List<SeriesRow>>,
    )

    private fun everywhereAuthor(
        query: String,
        index: SearchIndex,
        limit: Int,
    ): List<Author> {
        val authors =
            index.authors
                .fuzzy(query) { listOfNotNull(it.name, it.displayName) }
                .take(limit)
        val bookAuthors =
            index.books
                .fuzzy(query) {
                    listOfNotNull(
                        it.title,
                        it.displayTitle,
                        it.narrator,
                        index.seriesByBook[it.id]?.joinToString(",") { series -> series.displayedTitle },
                    )
                }.take(limit)
                .flatMap { index.authorsByBook[it.id].orEmpty() }
        return (authors + bookAuthors).distinctBy { it.id }.take(limit).map { it.toModel() }
    }

    context(_: Transaction)
    private fun everywhereSeries(
        query: String,
        index: SearchIndex,
        limit: Int,
    ): List<Series> {
        val series =
            index.series
                .fuzzy(query) { listOfNotNull(it.title, it.displayTitle) }
                .take(limit)
        val bookSeries =
            index.books
                .fuzzy(query) {
                    listOfNotNull(
                        it.title,
                        it.displayTitle,
                        it.narrator,
                        index.authorsByBook[it.id]?.joinToString(",") { author -> author.displayedName },
                    )
                }.take(limit)
                .flatMap { index.seriesByBook[it.id].orEmpty() }
        return seriesToModels((series + bookSeries).distinctBy { it.id }.take(limit))
    }

    context(_: Transaction)
    private fun everywhereBook(
        query: String,
        index: SearchIndex,
        limit: Int,
    ): List<Book> {
        val books =
            index.books
                .fuzzy(query) { listOfNotNull(it.title, it.displayTitle) }
                .take(limit)
        val booksAndOther =
            index.books
                .fuzzy(query) {
                    listOfNotNull(
                        index.authorsByBook[it.id]?.joinToString(", ") { author -> author.displayedName },
                        index.seriesByBook[it.id]?.joinToString(",") { series -> series.displayedTitle },
                        it.narrator,
                    )
                }.take(limit)
        return booksToModels((books + booksAndOther).distinctBy { it.id }.take(limit))
    }
}
