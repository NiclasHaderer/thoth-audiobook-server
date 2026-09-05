package io.thoth.server.repositories

import io.thoth.models.Author
import io.thoth.models.Book
import io.thoth.models.LibrarySearchResult
import io.thoth.models.Series
import io.thoth.server.common.extensions.fuzzy
import io.thoth.server.database.views.AuthorRow
import io.thoth.server.database.views.BookRow
import io.thoth.server.database.views.SeriesRow
import io.thoth.server.database.views.bookAuthors
import io.thoth.server.database.views.bookSeries
import io.thoth.server.database.views.booksToModels
import io.thoth.server.database.views.seriesToModels
import io.thoth.server.database.views.toAuthorRow
import io.thoth.server.database.views.toBookRow
import io.thoth.server.database.views.toSeriesRow
import io.thoth.server.database.views.AuthorMetadataView
import io.thoth.server.database.views.BookMetadataView
import io.thoth.server.database.views.SeriesMetadataView
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

// TODO we can improve the performance of this by a lot...
object SearchRepository {
    fun everywhere(
        query: String,
        libsToSearch: List<UUID>,
        userId: UUID,
        limit: Int = 5,
    ): LibrarySearchResult =
        transaction {
            val books =
                BookMetadataView
                    .selectAll()
                    .where { (BookMetadataView.library inList libsToSearch) and BookMetadataView.visible }
                    .map { it.toBookRow() }
            val authors =
                AuthorMetadataView
                    .selectAll()
                    .where { (AuthorMetadataView.library inList libsToSearch) and AuthorMetadataView.visible }
                    .map { it.toAuthorRow() }
            val series =
                SeriesMetadataView
                    .selectAll()
                    .where { (SeriesMetadataView.library inList libsToSearch) and SeriesMetadataView.visible }
                    .map { it.toSeriesRow() }

            val authorsById = authors.associateBy { it.id }
            val seriesById = series.associateBy { it.id }
            val bookIds = books.map { it.id }
            val authorsByBook =
                bookAuthors(bookIds).mapValues { (_, links) -> links.mapNotNull { authorsById[it.id] } }
            val seriesByBook =
                bookSeries(bookIds).mapValues { (_, links) -> links.mapNotNull { seriesById[it.id] } }

            val index = SearchIndex(books, authors, series, authorsByBook, seriesByBook)
            LibrarySearchResult(
                books = everywhereBook(query, index, userId, limit),
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
                .fuzzy(query) { listOf(it.name) }
                .take(limit)
        val bookAuthors =
            index.books
                .fuzzy(query) {
                    listOfNotNull(
                        it.title,
                        index.seriesByBook[it.id]?.joinToString(",") { series -> series.title },
                    ) + it.narrators
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
                .fuzzy(query) { listOf(it.title) }
                .take(limit)
        val bookSeries =
            index.books
                .fuzzy(query) {
                    listOfNotNull(
                        it.title,
                        index.authorsByBook[it.id]?.joinToString(",") { author -> author.name },
                    ) + it.narrators
                }.take(limit)
                .flatMap { index.seriesByBook[it.id].orEmpty() }
        return seriesToModels((series + bookSeries).distinctBy { it.id }.take(limit))
    }

    context(_: Transaction)
    private fun everywhereBook(
        query: String,
        index: SearchIndex,
        userId: UUID,
        limit: Int,
    ): List<Book> {
        val books =
            index.books
                .fuzzy(query) { listOf(it.title) }
                .take(limit)
        val booksAndOther =
            index.books
                .fuzzy(query) {
                    listOfNotNull(
                        index.authorsByBook[it.id]?.joinToString(", ") { author -> author.name },
                        index.seriesByBook[it.id]?.joinToString(",") { series -> series.title },
                    ) + it.narrators
                }.take(limit)
        return booksToModels((books + booksAndOther).distinctBy { it.id }.take(limit), userId)
    }
}
