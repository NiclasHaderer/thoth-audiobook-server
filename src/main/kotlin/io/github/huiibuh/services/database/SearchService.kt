package io.github.huiibuh.services.database

import io.github.huiibuh.db.tables.Author
import io.github.huiibuh.db.tables.Book
import io.github.huiibuh.db.tables.Series
import io.github.huiibuh.db.tables.TAuthors
import io.github.huiibuh.db.tables.TBooks
import io.github.huiibuh.db.tables.TSeries
import io.github.huiibuh.models.AuthorModel
import io.github.huiibuh.models.SearchModel
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.leftJoin
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
object SearchService {
    fun everywhere(query: String): SearchModel {
        val preparedQuery = prepareQuery(query)
        return SearchModel(
            books = everywhereBook(preparedQuery),
            series = listOf(), // TODO everywhereSeries(preparedQuery),
            authors = listOf() // TODO everywhereAuthor(preparedQuery)
        )
    }

    fun everywhereAuthor(strQuery: String): List<AuthorModel> = transaction {
        val query = TAuthors
                .select {
                    TAuthors.name like strQuery or
                            (TBooks.title like strQuery) or
                            (TBooks.description like strQuery) or
                            (TBooks.asin like strQuery)

                }
        Author.wrapRows(query).map { it.toModel() }
    }

    private fun everywhereSeries(strQuery: String) = transaction {
        val query = TSeries.apply {
        }.select {
            // Series of an author
            (TAuthors.name like strQuery) or
                    (TAuthors.biography like strQuery) or
                    // Series which match the query
                    (TSeries.title like strQuery) or
                    (TSeries.asin like strQuery) or
                    (TSeries.description like strQuery) or
                    // Series which match the books which match the query
                    (TBooks.title like strQuery) or
                    (TBooks.description like strQuery) or
                    (TBooks.asin like strQuery)
        }
        Series.wrapRows(query).map { it.toModel() }
    }

    private fun everywhereBook(strQuery: String) = transaction {
        val query = TBooks
                .leftJoin(TSeries, { TBooks.series }, { id })
                .leftJoin(TAuthors, { TBooks.author }, { id })
                .select {
                    (TAuthors.name like strQuery) or
                            (TSeries.title like strQuery) or
                            (TBooks.title like strQuery) or
                            (TBooks.description like strQuery) or
                            (TBooks.asin like strQuery) or
                            (TBooks.asin like strQuery)
                }
        Book.wrapRows(query).map { it.toModel() }
    }

    private fun prepareQuery(query: String): String {
        return "%${query.replace("%", "\\%")}%"
    }
}
