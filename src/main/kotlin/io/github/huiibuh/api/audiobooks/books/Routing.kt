package io.github.huiibuh.api.audiobooks.books

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.put
import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import io.github.huiibuh.api.ApiTags
import io.github.huiibuh.api.audiobooks.QueryLimiter
import io.github.huiibuh.db.tables.Book
import io.github.huiibuh.models.BookModel
import io.github.huiibuh.models.BookModelWithTracks
import io.github.huiibuh.models.PaginatedResponse
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

    put(body = OpenAPIPipelineResponseContext<BookModel>::patchBook)
}
