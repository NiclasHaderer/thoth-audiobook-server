package io.github.huiibuh.api.audiobooks.series

import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import com.papsign.ktor.openapigen.route.response.respond
import io.github.huiibuh.api.exceptions.APINotFound
import io.github.huiibuh.db.removeAllUnusedFromDb
import io.github.huiibuh.db.tables.ProviderID
import io.github.huiibuh.db.tables.Series
import io.github.huiibuh.db.tables.Track
import io.github.huiibuh.file.tagger.saveToFile
import io.github.huiibuh.file.tagger.toTrackModel
import io.github.huiibuh.models.SeriesModel
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.sql.transactions.transaction

internal suspend fun OpenAPIPipelineResponseContext<SeriesModel>.patchSeries(
    seriesId: SeriesId,
    patchSeries: PatchSeries,
) {
    val series = transaction {
        val series = Series.findById(seriesId.uuid) ?: throw APINotFound("Series could not be found")

        val tracks = Track.forSeries(seriesId.uuid)

        val trackReferences = tracks.toTrackModel()

        if (patchSeries.title != series.title) {
            series.title = patchSeries.title
            trackReferences.forEach { it.series = patchSeries.title }
        }

        val newProviderID = patchSeries.id
        if (ProviderID.eq(series.providerID, newProviderID)) {
            series.providerID = if (newProviderID == null) null else ProviderID.getOrCreate(newProviderID)
        }

        if (patchSeries.description != series.description) {
            series.description = patchSeries.description
        }
        trackReferences.saveToFile()
        flushCache()
        series
    }
    respond(series.toModel())
    removeAllUnusedFromDb()
}
