package io.thoth.server.api.audiobooks.series

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.patch
import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import io.thoth.database.tables.Series
import io.thoth.models.datastructures.PaginatedResponse
import io.thoth.models.datastructures.SeriesModel
import io.thoth.models.datastructures.SeriesModelWithBooks
import io.thoth.server.api.ApiTags
import io.thoth.server.api.audiobooks.QueryLimiter
import java.util.*


fun NormalOpenAPIRoute.registerSeriesRouting(path: String = "series") {
    route(path) {
        tag(ApiTags.Series) {
            routing()
        }
    }
}

internal fun NormalOpenAPIRoute.routing() {
    get<QueryLimiter, PaginatedResponse<SeriesModel>> {
        val series = Series.getMultiple(it.limit, it.offset)
        val seriesCount = Series.totalCount()
        val response = PaginatedResponse(series, total = seriesCount, offset = it.offset, limit = it.limit)
        respond(response)
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
