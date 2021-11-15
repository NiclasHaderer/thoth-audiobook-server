package io.github.huiibuh.api.audiobooks.albums

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.github.huiibuh.api.audiobooks.QueryLimiter
import io.github.huiibuh.db.tables.AlbumModel
import io.github.huiibuh.services.database.AlbumService
import io.github.huiibuh.services.database.CollectionService


fun NormalOpenAPIRoute.albumsRouting(path: String = "collections") {
    route(path) {
        routing()
    }
}

internal fun NormalOpenAPIRoute.routing() {
    get<QueryLimiter, List<AlbumModel>> {
        val t = AlbumService.getMultiple(it.limit, it.offset)
        respond(t)
    }
    get<AlbumId, AlbumModel> {
        val t = AlbumService.get(it.uuid)
        respond(t)
    }
}
