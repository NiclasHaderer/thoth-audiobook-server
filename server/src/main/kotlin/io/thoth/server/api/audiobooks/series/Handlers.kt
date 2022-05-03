package io.thoth.server.api.audiobooks.series

import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import io.thoth.common.exceptions.APINotImplemented
import io.thoth.models.SeriesModel

internal suspend fun OpenAPIPipelineResponseContext<SeriesModel>.patchSeries(
    seriesId: SeriesId,
    patchSeries: PatchSeries,
) {
    throw APINotImplemented("Series modification is not yet supported")
}
