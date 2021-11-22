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
import io.github.huiibuh.db.tables.Book
import io.github.huiibuh.db.tables.TBooks
import io.github.huiibuh.models.AuthorModel
import io.github.huiibuh.models.AuthorModelWithBooks
import io.github.huiibuh.services.database.AuthorService
import org.jetbrains.exposed.sql.transactions.transaction


fun NormalOpenAPIRoute.registerAuthorRouting(path: String = "authors") {
    route(path) {
        tag(ApiTags.Authors) {
            routing()
        }
    }
}

internal fun NormalOpenAPIRoute.routing() {
    get<QueryLimiter, List<AuthorModel>> {
        val t = AuthorService.getMultiple(it.limit, it.offset)
        respond(t)
    }
    get<AuthorId, AuthorModelWithBooks> {
        val author = AuthorService.get(it.uuid)
        val books = transaction { Book.find { TBooks.author eq it.uuid }.map { it.toModel() } }
        respond(AuthorModelWithBooks.fromModel(author, books))
    }
    patch(body = OpenAPIPipelineResponseContext<AuthorModel>::patchAuthor)
}
