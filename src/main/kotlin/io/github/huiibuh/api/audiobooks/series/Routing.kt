package io.github.huiibuh.api.audiobooks.series

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.patch
import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import io.github.huiibuh.api.ApiTags
import io.github.huiibuh.api.audiobooks.QueryLimiter
import io.github.huiibuh.models.SeriesModel
import io.github.huiibuh.models.SeriesModelWithBooks
import io.github.huiibuh.services.database.SeriesService


fun NormalOpenAPIRoute.registerSeriesRouting(path: String = "series") {
    route(path) {
        tag(ApiTags.Series) {
            routing()
        }
    }
}

internal fun NormalOpenAPIRoute.routing() {
    get<QueryLimiter, List<SeriesModel>> {
        respond(
            SeriesService.getMultiple(it.limit, it.offset)
        )
    }
    get<SeriesId, SeriesModelWithBooks> {
        respond(
            SeriesService.get(it.uuid)
        )
    }
    patch(body = OpenAPIPipelineResponseContext<SeriesModel>::patchSeries)
}
