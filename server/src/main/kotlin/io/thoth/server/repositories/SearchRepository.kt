package io.thoth.server.repositories

import io.thoth.models.LibrarySearchResult
import io.thoth.server.common.extensions.fuzzy
import io.thoth.server.database.tables.AuthorEntity
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.BookEntity
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.SeriesEntity
import io.thoth.server.database.tables.SeriesTable
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.core.*
import java.util.UUID

object SearchRepository {
    fun everywhere(
        query: String,
        libsToSearch: List<UUID>,
        limit: Int = 5,
    ): LibrarySearchResult =
        LibrarySearchResult(
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
            AuthorEntity
                .find { AuthorTable.library inList libsToSearch }
                .fuzzy(
                    query,
                ) { listOfNotNull(it.name) }
                .take(limit)
        val bookAuthors =
            BookEntity
                .find { BooksTable.library inList libsToSearch }
                .fuzzy(query) { listOfNotNull(it.title, it.narrator, it.series.joinToString(",") { it.title }) }
                .take(limit)
                .flatMap { it.authors }
        (authors + bookAuthors).distinctBy { it.id }.take(limit).map { it.toModel() }
    }

    private fun everywhereSeries(
        query: String,
        libsToSearch: List<UUID>,
        limit: Int,
    ) = transaction {
        val series =
            SeriesEntity
                .find { SeriesTable.library inList libsToSearch }
                .fuzzy(
                    query,
                ) { listOfNotNull(it.title) }
                .take(limit)
        val authorSeries =
            BookEntity
                .find { BooksTable.library inList libsToSearch }
                .fuzzy(query) { listOfNotNull(it.title, it.narrator, it.authors.joinToString(",") { it.name }) }
                .take(limit)
                .flatMap { it.series }
        (series + authorSeries).distinctBy { it.id }.take(limit).map { it.toModel() }
    }

    private fun everywhereBook(
        query: String,
        libsToSearch: List<UUID>,
        limit: Int,
    ) = transaction {
        val books =
            BookEntity
                .find { BooksTable.library inList libsToSearch }
                .fuzzy(
                    query,
                ) { listOfNotNull(it.title) }
                .take(limit)
        val booksAndOther =
            BookEntity
                .find { BooksTable.library inList libsToSearch }
                .fuzzy(query) {
                    listOfNotNull(
                        it.authors.joinToString(", ") { it.name },
                        it.series.joinToString(",") { it.title },
                        it.narrator,
                    )
                }.take(limit)
        (books + booksAndOther).distinctBy { it.id }.take(limit).map { it.toModel() }
    }
}
