package io.github.huiibuh.api.audiobooks.series

import api.exceptions.APINotFound
import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import com.papsign.ktor.openapigen.route.response.respond
import io.github.huiibuh.db.tables.Series
import io.github.huiibuh.db.tables.TTracks
import io.github.huiibuh.db.tables.Track
import io.github.huiibuh.models.SeriesModel
import io.github.huiibuh.scanner.toTrackModel
import io.github.huiibuh.services.RemoveEmpty
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.sql.transactions.transaction

internal suspend fun OpenAPIPipelineResponseContext<SeriesModel>.patchSeries(id: SeriesId, patchSeries: PatchSeries) {
    // TODO update logic
    val series = transaction {
        val series = Series.findById(id.uuid) ?: throw APINotFound("Series could not be found")

        val tracks = Track.find { TTracks.series eq id.uuid }.toList()
        val trackReferences = tracks.toTrackModel()

        if (patchSeries.title != null) {
            series.title = patchSeries.title
            trackReferences.forEach { it.series = patchSeries.title }
        }

        if (patchSeries.asin != null) {
            series.asin = patchSeries.asin
        }

        if (patchSeries.description != null) {
            series.description = patchSeries.description
        }
        flushCache()
        series
    }
    respond(series.toModel())
    RemoveEmpty.all()
}
