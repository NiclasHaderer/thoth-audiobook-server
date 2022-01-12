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
import io.github.huiibuh.db.tables.Series
import io.github.huiibuh.models.SeriesModel
import io.github.huiibuh.models.SeriesModelWithBooks
import java.util.*


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
            Series.getMultiple(it.limit, it.offset)
        )
    }
    route("sorting").get<QueryLimiter, List<UUID>> { query ->
        respond(
            Series.getMultiple(query.limit, query.offset).map { it.id }
        )
    }
    get<SeriesId, SeriesModelWithBooks> {
        respond(
            Series.getById(it.uuid)
        )
    }

    patch(body = OpenAPIPipelineResponseContext<SeriesModel>::patchSeries)
}
