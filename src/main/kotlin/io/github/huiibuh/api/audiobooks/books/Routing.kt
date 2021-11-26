package io.github.huiibuh.api.audiobooks.books

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.patch
import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import io.github.huiibuh.api.ApiTags
import io.github.huiibuh.api.audiobooks.QueryLimiter
import io.github.huiibuh.models.BookModel
import io.github.huiibuh.models.BookModelWithTracks
import io.github.huiibuh.services.database.BookService


fun NormalOpenAPIRoute.registerBookRouting(path: String = "books") {
    route(path) {
        tag(ApiTags.Books) {
            routing()
        }
    }
}

internal fun NormalOpenAPIRoute.routing() {
    get<QueryLimiter, List<BookModel>> {
        respond(
            BookService.getMultiple(it.limit, it.offset)
        )
    }
    get<BookId, BookModelWithTracks> {
        respond(
            BookService.get(it.uuid)
        )
    }

    patch(body = OpenAPIPipelineResponseContext<BookModel>::patchBook)
}
