package io.github.huiibuh.api.audiobooks.series

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.github.huiibuh.api.audiobooks.QueryLimiter
import io.github.huiibuh.models.BookModel
import io.github.huiibuh.models.SeriesModel
import io.github.huiibuh.services.database.SeriesService


fun NormalOpenAPIRoute.seriesRouting(path: String = "series") {
    route(path) {
        routing()
    }
}

internal fun NormalOpenAPIRoute.routing() {
    get<QueryLimiter, List<SeriesModel>> {
        val t = SeriesService.getSeries(it.limit, it.offset)
        respond(t)
    }
    get<SeriesId, SeriesModel> {
        val t = SeriesService.get(it.uuid)
        respond(t)
    }
    get<SeriesBooks, List<BookModel>> {
        val t = SeriesService.getBooks(it.uuid)
        respond(t)
    }
}
