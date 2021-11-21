package io.github.huiibuh.api.audiobooks.authors

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.github.huiibuh.api.audiobooks.QueryLimiter
import io.github.huiibuh.models.AuthorModel
import io.github.huiibuh.services.database.AuthorService


fun NormalOpenAPIRoute.authorRouting(path: String = "authors") {
    route(path) {
        routing()
    }
}

internal fun NormalOpenAPIRoute.routing() {
    get<QueryLimiter, List<AuthorModel>> {
        val t = AuthorService.getMultiple(it.limit, it.offset)
        respond(t)
    }
    get<AuthorId, AuthorModel> {
        val t = AuthorService.get(it.uuid)
        respond(t)
    }
}
