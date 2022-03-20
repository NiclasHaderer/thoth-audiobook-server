package io.github.huiibuh.api.audiobooks.series

import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import io.github.huiibuh.api.exceptions.APINotImplemented
import io.github.huiibuh.models.SeriesModel

internal suspend fun OpenAPIPipelineResponseContext<SeriesModel>.patchSeries(
    seriesId: SeriesId,
    patchSeries: PatchSeries,
) {
    throw APINotImplemented("Series modification is not yet supported")
}
