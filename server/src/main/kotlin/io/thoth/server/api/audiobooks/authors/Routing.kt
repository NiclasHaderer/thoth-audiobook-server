package io.thoth.server.api.audiobooks.authors

import io.ktor.http.*
import io.ktor.server.routing.*
import io.thoth.database.access.getDetailedById
import io.thoth.database.access.getMultiple
import io.thoth.database.access.positionOf
import io.thoth.database.tables.Author
import io.thoth.database.tables.TAuthors
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

fun Route.registerAuthorRouting() =
    route("authors") {
        get<QueryLimiter, PaginatedResponse<AuthorModel>> { (limit, offset) ->
            transaction {
                val books = Author.getMultiple(limit, offset)
                val seriesCount = Author.count()
                PaginatedResponse(books, total = seriesCount, offset = offset, limit = limit)
            }
        }
        get<QueryLimiter, List<UUID>>("sorting") { (limit, offset) ->
            transaction { Author.getMultiple(limit, offset).map { it.id } }
        }

        get<AuthorId.Position, Position> {
            transaction {
                val sortOrder =
                    Author.positionOf(it.parent.id) ?: serverError(HttpStatusCode.NotFound, "Author was not found")
                Position(sortIndex = sortOrder, id = it.parent.id, order = Position.Order.ASC)
            }
        }

        get<AuthorId, DetailedAuthorModel> { (id) ->
            transaction { Author.getDetailedById(id) ?: serverError(HttpStatusCode.NotFound, "Author was not found") }
        }

        get<AuthorName, List<NamedId>>("autocomplete") {
            transaction {
                Author.all().orderBy(TAuthors.name.lowerCase() to SortOrder.ASC).limit(30).map {
                    NamedId(it.id.value, it.name)
                }
            }
        }

        patch(RouteHandler::patchAuthor)

        post(RouteHandler::postAuthor)
    }
