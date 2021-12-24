package io.github.huiibuh.api.audiobooks.series

import api.exceptions.APINotFound
import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import com.papsign.ktor.openapigen.route.response.respond
import io.github.huiibuh.db.tables.Book
import io.github.huiibuh.db.tables.Series
import io.github.huiibuh.db.tables.TBooks
import io.github.huiibuh.db.tables.TTracks
import io.github.huiibuh.db.tables.Track
import io.github.huiibuh.models.SeriesModel
import io.github.huiibuh.scanner.toTrackModel
import io.github.huiibuh.services.RemoveEmpty
import io.github.huiibuh.services.database.TrackService
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.sql.transactions.transaction

internal suspend fun OpenAPIPipelineResponseContext<SeriesModel>.patchSeries(
    seriesId: SeriesId,
    patchSeries: PatchSeries,
) {
    val series = transaction {
        val series = Series.findById(seriesId.uuid) ?: throw APINotFound("Series could not be found")

        val tracks = TrackService.forSeries(seriesId.uuid)

        val trackReferences = tracks.toTrackModel()

        if (patchSeries.title != series.title) {
            series.title = patchSeries.title
            trackReferences.forEach { it.series = patchSeries.title }
        }

        if (patchSeries.asin != series.asin) {
            series.asin = patchSeries.asin
        }

        if (patchSeries.description != series.description) {
            series.description = patchSeries.description
        }
        flushCache()
        series
    }
    respond(series.toModel())
    RemoveEmpty.all()
}
