package io.thoth.server.api.audiobooks.books

import io.ktor.http.*
import io.ktor.server.routing.*
import io.thoth.database.tables.Book
import io.thoth.models.BookModel
import io.thoth.models.BookModelWithTracks
import io.thoth.models.PaginatedResponse
import io.thoth.openapi.routing.RouteHandler
import io.thoth.openapi.routing.get
import io.thoth.openapi.routing.patch
import io.thoth.openapi.serverError
import io.thoth.server.api.audiobooks.QueryLimiter
import java.util.*


fun Route.registerBookRouting(path: String = "books") {
    route(path) {
        routing()
    }
}

internal fun Route.routing() {
    get<QueryLimiter, PaginatedResponse<BookModel>> {
        val books = Book.getMultiple(it.limit, it.offset)
        val seriesCount = Book.totalCount()
        PaginatedResponse(books, total = seriesCount, offset = it.offset, limit = it.limit)
    }

    get<QueryLimiter, List<UUID>>("sorting") { query ->
        Book.getMultiple(query.limit, query.offset).map { it.id }
    }

    get<BookId, BookModelWithTracks> {
        Book.getById(it.id) ?: serverError(HttpStatusCode.NotFound, "Could not find book")
    }

    patch(RouteHandler::patchBook)
}
