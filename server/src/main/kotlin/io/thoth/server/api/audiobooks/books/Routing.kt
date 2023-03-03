package io.thoth.server.api.audiobooks.books

import io.ktor.http.*
import io.ktor.server.routing.*
import io.thoth.database.access.getDetailedById
import io.thoth.database.access.getMultiple
import io.thoth.database.access.positionOf
import io.thoth.database.tables.Book
import io.thoth.database.tables.TBooks
import io.thoth.models.*
import io.thoth.openapi.routing.RouteHandler
import io.thoth.openapi.routing.get
import io.thoth.openapi.routing.patch
import io.thoth.openapi.routing.post
import io.thoth.openapi.serverError
import io.thoth.server.api.audiobooks.QueryLimiter
import java.util.*
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.registerBookRouting() =
    route("books") {
        get<QueryLimiter, PaginatedResponse<BookModel>> {
            transaction {
                val books = Book.getMultiple(it.limit, it.offset)
                val total = Book.count()
                PaginatedResponse(books, total = total, offset = it.offset, limit = it.limit)
            }
        }

        get<QueryLimiter, List<UUID>>("sorting") { query ->
            transaction { Book.getMultiple(query.limit, query.offset).map { it.id } }
        }

        get<BookId.Position, Position> {
            val sortOrder =
                transaction { Book.positionOf(it.parent.id) }
                    ?: serverError(HttpStatusCode.NotFound, "Could not find book")
            Position(sortIndex = sortOrder, id = it.parent.id, order = Position.Order.ASC)
        }

        get<BookId, DetailedBookModel> {
            transaction { Book.getDetailedById(it.id) }
                ?: serverError(HttpStatusCode.NotFound, "Could not find book")
        }

        get<BookName, List<TitledId>>("autocomplete") {
            transaction {
                Book.all().orderBy(TBooks.title.lowerCase() to SortOrder.ASC).limit(30).map {
                    TitledId(it.id.value, it.title)
                }
            }
        }

        patch(RouteHandler::patchBook)

        post(RouteHandler::postBook)
    }
