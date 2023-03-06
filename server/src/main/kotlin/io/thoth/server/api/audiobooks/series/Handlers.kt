package io.thoth.server.api.audiobooks.series

import io.ktor.http.*
import io.thoth.common.extensions.toSizedIterable
import io.thoth.database.access.getNewImage
import io.thoth.database.access.toModel
import io.thoth.database.tables.Author
import io.thoth.database.tables.Book
import io.thoth.database.tables.Image
import io.thoth.database.tables.Series
import io.thoth.models.SeriesModel
import io.thoth.openapi.routing.RouteHandler
import io.thoth.openapi.serverError
import org.jetbrains.exposed.sql.transactions.transaction

fun RouteHandler.patchSeries(
    seriesId: SeriesId,
    patchSeries: PatchSeries,
): SeriesModel = transaction {
    val series = Series.findById(seriesId.id) ?: serverError(HttpStatusCode.NotFound, "Could not find series")

    series.apply {
        title = patchSeries.title ?: title
        provider = patchSeries.provider ?: provider
        providerID = patchSeries.providerID ?: providerID
        totalBooks = patchSeries.totalBooks ?: totalBooks
        primaryWorks = patchSeries.primaryWorks ?: primaryWorks
        coverID = Image.getNewImage(patchSeries.cover, currentImageID = coverID, default = coverID)
        description = patchSeries.description ?: description
    }

    if (patchSeries.authors != null) {
        series.authors =
            patchSeries.authors
                .map { Author.findById(it) ?: serverError(HttpStatusCode.NotFound, "Could not find author") }
                .toSizedIterable()
    }

    if (patchSeries.books != null) {
        series.books =
            patchSeries.books
                .map { Book.findById(it) ?: serverError(HttpStatusCode.NotFound, "Could not find book") }
                .toSizedIterable()
    }

    series.toModel()
}

fun RouteHandler.postSeries(
    seriesId: SeriesId,
    postSeries: PostSeries,
) = transaction {
    val series = Series.findById(seriesId.id) ?: serverError(HttpStatusCode.NotFound, "Could not find series")

    series.apply {
        title = postSeries.title
        provider = postSeries.provider
        providerID = postSeries.providerID
        totalBooks = postSeries.totalBooks
        primaryWorks = postSeries.primaryWorks
        coverID = Image.getNewImage(postSeries.cover, currentImageID = coverID, default = null)
        description = postSeries.description
        series.authors =
            postSeries.authors
                .map { Author.findById(it) ?: serverError(HttpStatusCode.NotFound, "Could not find author") }
                .toSizedIterable()
        series.books =
            postSeries.books
                .map { Book.findById(it) ?: serverError(HttpStatusCode.NotFound, "Could not find book") }
                .toSizedIterable()
    }
    series.toModel()
}
