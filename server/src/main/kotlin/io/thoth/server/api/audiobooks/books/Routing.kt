package io.thoth.server.api.audiobooks.books

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.patch
import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import io.thoth.database.tables.Book
import io.thoth.models.BookModel
import io.thoth.models.BookModelWithTracks
import io.thoth.models.PaginatedResponse
import io.thoth.server.api.ApiTags
import io.thoth.server.api.audiobooks.QueryLimiter
import java.util.*


fun NormalOpenAPIRoute.registerBookRouting(path: String = "books") {
    route(path) {
        tag(ApiTags.Books) {
            routing()
        }
    }
}

internal fun NormalOpenAPIRoute.routing() {
    get<QueryLimiter, PaginatedResponse<BookModel>> {
        val books = Book.getMultiple(it.limit, it.offset)
        val seriesCount = Book.totalCount()
        val response = PaginatedResponse(books, total = seriesCount, offset = it.offset, limit = it.limit)
        respond(response)
    }
    route("sorting").get<QueryLimiter, List<UUID>> { query ->
        respond(
            Book.getMultiple(query.limit, query.offset).map { it.id }
        )
    }
    get<BookId, BookModelWithTracks> {
        respond(
            Book.getById(it.uuid)
        )
    }

    patch(body = OpenAPIPipelineResponseContext<BookModel>::patchBook)
}
