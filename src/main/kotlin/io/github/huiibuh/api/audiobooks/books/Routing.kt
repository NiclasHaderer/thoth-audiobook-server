package io.github.huiibuh.api.audiobooks.books

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.github.huiibuh.api.audiobooks.QueryLimiter
import io.github.huiibuh.models.BookModel
import io.github.huiibuh.services.database.BookService


fun NormalOpenAPIRoute.bookRouting(path: String = "books") {
    route(path) {
        routing()
    }
}

internal fun NormalOpenAPIRoute.routing() {
    get<QueryLimiter, List<BookModel>> {
        val t = BookService.getMultiple(it.limit, it.offset)
        respond(t)
    }
    get<BookId, BookModel> {
        val t = BookService.get(it.uuid)
        respond(t)
    }
}
