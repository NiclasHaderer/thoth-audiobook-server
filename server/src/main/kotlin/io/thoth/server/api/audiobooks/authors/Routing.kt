package io.thoth.server.api.audiobooks.authors

import io.ktor.http.*
import io.ktor.server.routing.*
import io.thoth.database.tables.Author
import io.thoth.models.AuthorModel
import io.thoth.models.AuthorModelWithBooks
import io.thoth.models.PaginatedResponse
import io.thoth.openapi.routing.RouteHandler
import io.thoth.openapi.routing.get
import io.thoth.openapi.routing.patch
import io.thoth.openapi.serverError
import io.thoth.server.api.audiobooks.QueryLimiter
import io.thoth.server.db.access.getDetailedById
import io.thoth.server.db.access.getMultiple
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

    get<AuthorId, AuthorModelWithBooks> {
        transaction { Author.getDetailedById(it.id) } ?: serverError(HttpStatusCode.NotFound, "Author was not found")
    }

    patch(RouteHandler::patchAuthor)
}
