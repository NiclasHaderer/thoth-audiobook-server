package io.thoth.server.api.audiobooks.authors

import io.ktor.http.*
import io.ktor.server.routing.*
import io.thoth.database.access.getDetailedById
import io.thoth.database.access.getMultiple
import io.thoth.database.access.positionOf
import io.thoth.database.tables.Author
import io.thoth.models.AuthorModel
import io.thoth.models.DetailedAuthorModel
import io.thoth.models.PaginatedResponse
import io.thoth.models.Position
import io.thoth.openapi.routing.RouteHandler
import io.thoth.openapi.routing.get
import io.thoth.openapi.routing.patch
import io.thoth.openapi.routing.post
import io.thoth.openapi.serverError
import io.thoth.server.api.audiobooks.QueryLimiter
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*


fun Route.registerAuthorRouting(path: String = "authors") {
    route(path) {
        routing()
    }
}

internal fun Route.routing() {
    get<QueryLimiter, PaginatedResponse<AuthorModel>> {
        transaction {
            val books = Author.getMultiple(it.limit, it.offset)
            val seriesCount = Author.count()
            PaginatedResponse(books, total = seriesCount, offset = it.offset, limit = it.limit)
        }
    }
    get<QueryLimiter, List<UUID>>("sorting") { query ->
        Author.getMultiple(query.limit, query.offset).map { it.id }
    }

    get<AuthorId.Position, Position> {
        val sortOrder =
            transaction { Author.positionOf(it.parent.id) } ?: serverError(
                HttpStatusCode.NotFound,
                "Author was not found"
            )
        Position(sortIndex = sortOrder, id = it.parent.id, order = Position.Order.ASC)
    }

    get<AuthorId, DetailedAuthorModel> {
        transaction { Author.getDetailedById(it.id) } ?: serverError(HttpStatusCode.NotFound, "Author was not found")
    }

    patch(RouteHandler::patchAuthor)

    post(RouteHandler::postAuthor)
}
