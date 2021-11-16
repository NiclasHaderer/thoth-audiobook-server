package io.github.huiibuh.api.audiobooks.artists

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.github.huiibuh.api.audiobooks.QueryLimiter
import io.github.huiibuh.models.ArtistModel
import io.github.huiibuh.services.database.ArtistService


fun NormalOpenAPIRoute.artistsRouting(path: String = "artists") {
    route(path) {
        routing()
    }
}

internal fun NormalOpenAPIRoute.routing() {
    get<QueryLimiter, List<ArtistModel>> {
        val t = ArtistService.getMultiple(it.limit, it.offset)
        respond(t)
    }
    get<ArtistId, ArtistModel> {
        val t = ArtistService.get(it.uuid)
        respond(t)
    }
}
