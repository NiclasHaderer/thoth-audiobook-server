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
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder
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
            series = everywhereSeries(preparedQuery),
            authors = everywhereAuthor(preparedQuery)
        )
    }

    fun everywhereAuthor(strQuery: String): List<AuthorModel> = transaction {
        val query = TAuthors
                .leftJoin(TSeries, { TAuthors.id }, { TSeries.author })
                .leftJoin(TBooks, { TAuthors.id }, { TBooks.author })
                .select { everywhereQuery(strQuery) }
        Author.wrapRows(query).distinctBy { it.id }.map { it.toModel() }
    }

    private fun everywhereSeries(strQuery: String) = transaction {
        val query = TSeries
                .leftJoin(TAuthors, { TSeries.author }, { TAuthors.id })
                .leftJoin(TBooks, { TSeries.id }, { TBooks.series })
                .select { everywhereQuery(strQuery) }
        Series.wrapRows(query).distinctBy { it.id }.map { it.toModel() }
    }

    private fun everywhereBook(strQuery: String) = transaction {
        val query = TBooks
                .leftJoin(TSeries, { TBooks.series }, { id })
                .leftJoin(TAuthors, { TBooks.author }, { id })
                .select { everywhereQuery(strQuery) }
        Book.wrapRows(query).distinctBy { it.id }.map { it.toModel() }
    }

    private fun prepareQuery(query: String): String {
        return "%${query.replace("%", "\\%")}%"
    }

    private fun everywhereQuery(strQuery: String) = buildExpression {
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

    private fun buildExpression(where: SqlExpressionBuilder.() -> Op<Boolean>): Op<Boolean> {
        return SqlExpressionBuilder.where()
    }

}

