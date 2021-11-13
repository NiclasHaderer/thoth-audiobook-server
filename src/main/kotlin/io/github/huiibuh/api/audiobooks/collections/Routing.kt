package io.github.huiibuh.api.audiobooks.collections

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.github.huiibuh.db.models.CollectionModel
import io.github.huiibuh.services.database.CollectionService


fun NormalOpenAPIRoute.collectionsRouting(path: String = "collections") {
    route(path) {
        routing()
    }
}

internal fun NormalOpenAPIRoute.routing() {
    get<Unit, List<CollectionModel>> {
        val t = CollectionService.getCollections(10, 0)
        respond(t)
    }
}
