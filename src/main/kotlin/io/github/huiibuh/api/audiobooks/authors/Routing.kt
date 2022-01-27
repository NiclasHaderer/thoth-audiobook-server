package io.github.huiibuh.api.audiobooks.authors

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.patch
import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import io.github.huiibuh.api.ApiTags
import io.github.huiibuh.api.audiobooks.QueryLimiter
import io.github.huiibuh.db.tables.Author
import io.github.huiibuh.models.AuthorModel
import io.github.huiibuh.models.AuthorModelWithBooks
import io.github.huiibuh.models.PaginatedResponse
import java.util.*


fun NormalOpenAPIRoute.registerAuthorRouting(path: String = "authors") {
    route(path) {
        tag(ApiTags.Authors) {
            routing()
        }
    }
}

internal fun NormalOpenAPIRoute.routing() {
    get<QueryLimiter, PaginatedResponse<AuthorModel>> {
        val books = Author.getMultiple(it.limit, it.offset)
        val seriesCount = Author.totalCount()
        val response = PaginatedResponse(books, total = seriesCount, offset = it.offset, limit = it.limit)
        respond(response)
    }
    route("sorting").get<QueryLimiter, List<UUID>> { query ->
        respond(
            Author.getMultiple(query.limit, query.offset).map { it.id }
        )
    }
    get<AuthorId, AuthorModelWithBooks> {
        respond(
            Author.getById(it.uuid)
        )
    }

    patch(body = OpenAPIPipelineResponseContext<AuthorModel>::patchAuthor)
}
