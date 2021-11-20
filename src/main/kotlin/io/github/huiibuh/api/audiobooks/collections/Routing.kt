package io.github.huiibuh.api.audiobooks.collections

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.github.huiibuh.api.audiobooks.QueryLimiter
import io.github.huiibuh.models.AlbumModel
import io.github.huiibuh.models.CollectionModel
import io.github.huiibuh.services.database.CollectionService


fun NormalOpenAPIRoute.collectionsRouting(path: String = "collections") {
    route(path) {
        routing()
    }
}

internal fun NormalOpenAPIRoute.routing() {
    get<QueryLimiter, List<CollectionModel>> {
        val t = CollectionService.getCollections(it.limit, it.offset)
        respond(t)
    }
    get<CollectionId, CollectionModel> {
        val t = CollectionService.get(it.uuid)
        respond(t)
    }
    get<CollectionAlbums, List<AlbumModel>> {
        val t = CollectionService.getBooks(it.uuid)
        respond(t)
    }
}
