package io.thoth.server.services

import io.thoth.generators.openapi.errors.ErrorResponse
import io.thoth.models.DetailedSeriesModel
import io.thoth.models.SeriesModel
import io.thoth.server.api.PartialSeriesApiModel
import io.thoth.server.api.SeriesApiModel
import io.thoth.server.common.extensions.toSizedIterable
import io.thoth.server.database.access.getNewImage
import io.thoth.server.database.access.toModel
import io.thoth.server.database.tables.Image
import io.thoth.server.database.tables.Series
import io.thoth.server.database.tables.TSeries
import java.util.*
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface SeriesRepository :
    Repository<Series, SeriesModel, DetailedSeriesModel, PartialSeriesApiModel, SeriesApiModel> {
    fun findByName(seriesTitle: String, libraryId: UUID): Series?
}

class SeriesRepositoryImpl() : SeriesRepository, KoinComponent {
    private val authorRepository by inject<AuthorRepository>()
    private val bookRepository by inject<BookRepository>()
    override fun findByName(seriesTitle: String, libraryId: UUID): Series? = transaction {
        Series.find { TSeries.title eq seriesTitle and (TSeries.library eq libraryId) }.firstOrNull()
    }

    override fun raw(id: UUID, libraryId: UUID): Series = transaction {
        Series.find { TSeries.id eq id and (TSeries.library eq libraryId) }.firstOrNull()
            ?: throw ErrorResponse.notFound("Series", id)
    }

    override fun get(id: UUID, libraryId: UUID): DetailedSeriesModel = transaction {
        val series = raw(id, libraryId)

        DetailedSeriesModel.fromModel(
            series = series.toModel(),
            books = series.books.orderBy(TSeries.title.lowerCase() to SortOrder.ASC).map { it.toModel() },
        )
    }

    override fun getAll(libraryId: UUID, order: SortOrder, limit: Int, offset: Long): List<SeriesModel> = transaction {
        Series.find { TSeries.library eq libraryId }
            .orderBy(TSeries.title.lowerCase() to order)
            .limit(limit, offset)
            .map { it.toModel() }
    }

    override fun search(query: String, libraryId: UUID): List<SeriesModel> = transaction {
        Series.find { TSeries.title like "%$query%" and (TSeries.library eq libraryId) }
            .orderBy(TSeries.title.lowerCase() to SortOrder.ASC)
            .limit(searchLimit)
            .map { it.toModel() }
    }

    override fun search(query: String): List<SeriesModel> = transaction {
        Series.find { TSeries.title like "%$query%" }
            .orderBy(TSeries.title.lowerCase() to SortOrder.ASC)
            .limit(searchLimit)
            .map { it.toModel() }
    }

    override fun sorting(libraryId: UUID, order: SortOrder, limit: Int, offset: Long): List<UUID> = transaction {
        Series.find { TSeries.library eq libraryId }
            .orderBy(TSeries.title.lowerCase() to order)
            .limit(limit, offset)
            .map { it.id.value }
    }

    override fun position(id: UUID, libraryId: UUID, order: SortOrder): Long = transaction {
        Series.find { TSeries.library eq libraryId }
            .orderBy(TSeries.title.lowerCase() to order)
            .indexOfFirst { it.id.value == id }
            .toLong()
    }

    override fun modify(id: UUID, libraryId: UUID, partial: PartialSeriesApiModel): SeriesModel = transaction {
        val series = raw(id, libraryId)

        series.apply {
            title = partial.title ?: title
            provider = partial.provider ?: provider
            providerID = partial.providerID ?: providerID
            totalBooks = partial.totalBooks ?: totalBooks
            primaryWorks = partial.primaryWorks ?: primaryWorks
            coverID = Image.getNewImage(partial.cover, currentImageID = coverID, default = coverID)
            description = partial.description ?: description
        }

        if (partial.authors != null) {
            series.authors = partial.authors.map { authorRepository.raw(it, libraryId) }.toSizedIterable()
        }

        if (partial.books != null) {
            series.books = partial.books.map { bookRepository.raw(it, libraryId) }.toSizedIterable()
        }

        series.toModel()
    }

    override fun replace(id: UUID, libraryId: UUID, complete: SeriesApiModel): SeriesModel = transaction {
        val series = raw(id, libraryId)

        series.apply {
            title = complete.title
            provider = complete.provider
            providerID = complete.providerID
            totalBooks = complete.totalBooks
            primaryWorks = complete.primaryWorks
            coverID = Image.getNewImage(complete.cover, currentImageID = coverID, default = null)
            description = complete.description
            series.authors = complete.authors.map { authorRepository.raw(it, libraryId) }.toSizedIterable()
            series.books = complete.books.map { bookRepository.raw(it, libraryId) }.toSizedIterable()
        }
        series.toModel()
    }

    override fun total(libraryId: UUID): Long = transaction { Series.find { TSeries.library eq libraryId }.count() }
}
