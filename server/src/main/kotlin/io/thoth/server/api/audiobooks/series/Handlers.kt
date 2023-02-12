package io.thoth.server.api.audiobooks.series

import io.ktor.http.*
import io.thoth.database.access.getNewImage
import io.thoth.database.access.toModel
import io.thoth.database.tables.Image
import io.thoth.database.tables.Series
import io.thoth.models.SeriesModel
import io.thoth.openapi.routing.RouteHandler
import io.thoth.openapi.serverError
import org.jetbrains.exposed.sql.transactions.transaction

internal fun RouteHandler.patchSeries(
    seriesId: SeriesId,
    patchSeries: PatchSeries,
): SeriesModel {
    val series =
        transaction { Series.findById(seriesId.id) } ?: serverError(HttpStatusCode.NotFound, "Could not find series")

    return transaction {
        series.apply {
            title = patchSeries.title ?: title
            provider = patchSeries.provider ?: provider
            providerID = patchSeries.providerID ?: providerID
            totalBooks = patchSeries.totalBooks ?: totalBooks
            primaryWorks = patchSeries.primaryWorks ?: primaryWorks
            coverID = Image.getNewImage(patchSeries.cover, currentImageID = coverID, default = coverID)
            description = patchSeries.description ?: description
        }
        series.toModel()
    }
}

internal fun RouteHandler.postSeries(
    seriesId: SeriesId,
    postSeries: PostSeries,
) {
    val series =
        transaction { Series.findById(seriesId.id) } ?: serverError(HttpStatusCode.NotFound, "Could not find series")

    return transaction {
        series.apply {
            title = postSeries.title
            provider = postSeries.provider
            providerID = postSeries.providerID
            totalBooks = postSeries.totalBooks
            primaryWorks = postSeries.primaryWorks
            coverID = Image.getNewImage(postSeries.cover, currentImageID = coverID, default = null)
            description = postSeries.description
        }
        series.toModel()
    }
}