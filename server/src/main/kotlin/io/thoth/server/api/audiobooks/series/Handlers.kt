package io.thoth.server.api.audiobooks.series

import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import io.thoth.models.datastructures.SeriesModel
import io.thoth.models.exceptions.APINotImplemented

internal suspend fun OpenAPIPipelineResponseContext<SeriesModel>.patchSeries(
    seriesId: SeriesId,
    patchSeries: PatchSeries,
) {
    throw APINotImplemented("Series modification is not yet supported")
}
