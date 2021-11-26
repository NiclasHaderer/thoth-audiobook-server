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
    fun everywhere(query: String, limit: Int = 5): SearchModel {
        val preparedQuery = prepareQuery(query)
        return SearchModel(
            books = everywhereBook(preparedQuery, limit),
            series = everywhereSeries(preparedQuery, limit),
            authors = everywhereAuthor(preparedQuery, limit),
        )
    }

    fun everywhereAuthor(strQuery: String, limit: Int): List<AuthorModel> = transaction {
        val query = TAuthors
                .leftJoin(TSeries, { TAuthors.id }, { TSeries.author })
                .leftJoin(TBooks, { TAuthors.id }, { TBooks.author })
                .select { everywhereQuery(strQuery) }
        Author.wrapRows(query).limit(limit).sortedBy { it.name }
                .distinctBy { it.id }.map { it.toModel() }
    }

    private fun everywhereSeries(strQuery: String, limit: Int) = transaction {
        val query = TSeries
                .leftJoin(TAuthors, { TSeries.author }, { TAuthors.id })
                .leftJoin(TBooks, { TSeries.id }, { TBooks.series })
                .select { everywhereQuery(strQuery) }
        Series.wrapRows(query).limit(limit).sortedBy { it.title }
                .distinctBy { it.id }.map { it.toModel() }
    }

    private fun everywhereBook(strQuery: String, limit: Int) = transaction {
        val query = TBooks
                .leftJoin(TSeries, { TBooks.series }, { id })
                .leftJoin(TAuthors, { TBooks.author }, { id })
                .select { everywhereQuery(strQuery) }
        Book.wrapRows(query).limit(limit).sortedBy { it.title }
                .distinctBy { it.id }.map { it.toModel() }
    }

    private fun prepareQuery(query: String): String {
        return "%${query.replace("%", "\\%")}%"
    }

    private fun everywhereQuery(strQuery: String) = buildExpression {
        // Authors
        (TAuthors.name like strQuery) or
                (TAuthors.biography like strQuery) or
                // Series
                (TSeries.title like strQuery) or
                (TSeries.asin like strQuery) or
                (TSeries.description like strQuery) or
                // Books
                (TBooks.title like strQuery) or
                (TBooks.description like strQuery) or
                (TBooks.asin like strQuery)
    }

    private fun buildExpression(where: SqlExpressionBuilder.() -> Op<Boolean>): Op<Boolean> {
        return SqlExpressionBuilder.where()
    }

}

