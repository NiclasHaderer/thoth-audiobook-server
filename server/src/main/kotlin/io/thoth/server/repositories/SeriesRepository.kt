package io.thoth.server.repositories

import io.thoth.metadata.MetadataProviders
import io.thoth.metadata.MetadataWrapper
import io.thoth.models.DetailedSeries
import io.thoth.models.Series
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.api.PartialSeriesApiModel
import io.thoth.server.api.SeriesApiModel
import io.thoth.server.common.extensions.add
import io.thoth.server.common.extensions.toSizedIterable
import io.thoth.server.database.access.getNewImage
import io.thoth.server.database.tables.AuthorEntity
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.ImageEntity
import io.thoth.server.database.tables.SeriesEntity
import io.thoth.server.database.tables.SeriesTable
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging.logger
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.jdbc.SizedCollection
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID

interface SeriesRepository :
    Repository<SeriesEntity, Series, DetailedSeries, PartialSeriesApiModel, SeriesApiModel> {
    fun findByName(
        seriesTitle: String,
        libraryId: UUID,
    ): SeriesEntity?

    fun getOrCreate(
        seriesName: String,
        libraryId: UUID,
        dbAuthor: List<AuthorEntity>,
    ): SeriesEntity

    fun create(
        seriesName: String,
        libraryId: UUID,
        dbAuthor: List<AuthorEntity>,
    ): SeriesEntity
}

class SeriesRepositoryImpl :
    SeriesRepository,
    KoinComponent {
    private val authorRepository by inject<AuthorRepository>()
    private val bookRepository by inject<BookRepository>()
    private val libraryRepository by inject<LibraryRepository>()
    private val metadataProviders by inject<MetadataProviders>()

    private companion object {
        val log = logger {}
    }

    override fun findByName(
        seriesTitle: String,
        libraryId: UUID,
    ): SeriesEntity? =
        transaction {
            SeriesEntity.find { SeriesTable.title eq seriesTitle and (SeriesTable.library eq libraryId) }.firstOrNull()
        }

    override fun raw(
        id: UUID,
        libraryId: UUID,
    ): SeriesEntity =
        SeriesEntity.find { SeriesTable.id eq id and (SeriesTable.library eq libraryId) }.firstOrNull()
            ?: throw ErrorResponse.notFound("Series", id)

    override fun get(
        id: UUID,
        libraryId: UUID,
    ): DetailedSeries =
        transaction {
            val series = raw(id = id, libraryId = libraryId)

            DetailedSeries.fromModel(
                series = series.toModel(),
                books = series.books.orderBy(BooksTable.title.lowerCase() to SortOrder.ASC).map { it.toModel() },
            )
        }

    override fun getAll(
        libraryId: UUID,
        order: SortOrder,
        limit: Int,
        offset: Long,
    ): List<Series> =
        transaction {
            SeriesEntity
                .find { SeriesTable.library eq libraryId }
                .orderBy(SeriesTable.title.lowerCase() to order)
                .offset(offset)
                .limit(limit)
                .map { it.toModel() }
        }

    override fun search(
        query: String,
        libraryId: UUID,
    ): List<Series> =
        transaction {
            SeriesEntity
                .find { SeriesTable.title like "%$query%" and (SeriesTable.library eq libraryId) }
                .orderBy(SeriesTable.title.lowerCase() to SortOrder.ASC)
                .limit(searchLimit)
                .map { it.toModel() }
        }

    override fun search(query: String): List<Series> =
        transaction {
            SeriesEntity
                .find { SeriesTable.title like "%$query%" }
                .orderBy(SeriesTable.title.lowerCase() to SortOrder.ASC)
                .limit(searchLimit)
                .map { it.toModel() }
        }

    override fun getOrCreate(
        seriesName: String,
        libraryId: UUID,
        dbAuthor: List<AuthorEntity>,
    ): SeriesEntity {
        val series = findByName(seriesName, libraryId)
        return if (series != null) {
            series.authors = series.authors.add(dbAuthor)
            series
        } else {
            create(seriesName, libraryId, dbAuthor)
        }
    }

    override fun create(
        seriesName: String,
        libraryId: UUID,
        dbAuthor: List<AuthorEntity>,
    ): SeriesEntity {
        log.info("Created series: $seriesName")
        return SeriesEntity.new {
            title = seriesName
            displayTitle = seriesName
            authors = SizedCollection(dbAuthor)
            library = libraryRepository.raw(libraryId)
        }
    }

    override fun sorting(
        libraryId: UUID,
        order: SortOrder,
        limit: Int,
        offset: Long,
    ): List<UUID> =
        transaction {
            SeriesEntity
                .find { SeriesTable.library eq libraryId }
                .orderBy(SeriesTable.title.lowerCase() to order)
                .offset(offset)
                .limit(limit)
                .map { it.id.value }
        }

    override fun position(
        id: UUID,
        libraryId: UUID,
        order: SortOrder,
    ): Long =
        transaction {
            SeriesEntity
                .find { SeriesTable.library eq libraryId }
                .orderBy(SeriesTable.title.lowerCase() to order)
                .indexOfFirst { it.id.value == id }
                .toLong()
        }

    override fun modify(
        id: UUID,
        libraryId: UUID,
        partial: PartialSeriesApiModel,
    ): Series =
        transaction {
            val series = raw(id, libraryId)

            series.apply {
                displayTitle = partial.title
                provider = partial.provider ?: provider
                providerID = partial.providerID ?: providerID
                totalBooks = partial.totalBooks ?: totalBooks
                primaryWorks = partial.primaryWorks ?: primaryWorks
                coverID = ImageEntity.getNewImage(partial.cover, currentImageID = coverID, default = coverID)
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

    override fun replace(
        id: UUID,
        libraryId: UUID,
        complete: SeriesApiModel,
    ): Series =
        transaction {
            val series = raw(id, libraryId)

            series.apply {
                title = complete.title
                provider = complete.provider
                providerID = complete.providerID
                totalBooks = complete.totalBooks
                primaryWorks = complete.primaryWorks
                coverID = ImageEntity.getNewImage(complete.cover, currentImageID = coverID, default = null)
                description = complete.description
                series.authors = complete.authors.map { authorRepository.raw(it, libraryId) }.toSizedIterable()
                series.books = complete.books.map { bookRepository.raw(it, libraryId) }.toSizedIterable()
            }
            series.toModel()
        }

    override fun autoMatch(
        id: UUID,
        libraryId: UUID,
    ): Series =
        transaction {
            val series = raw(id, libraryId)
            val library = libraryRepository.raw(libraryId)

            val metadataAgents =
                library.metadataScanners.mapNotNull { agent -> metadataProviders.find { it.uniqueName == agent.name } }
            val metadataWrapper = MetadataWrapper(metadataAgents)
            val seriesMetadata =
                runBlocking {
                    metadataWrapper
                        .getSeriesByName(series.title, library.language, series.authors.joinToString(", ") { it.name })
                        .firstOrNull()
                } ?: return@transaction series.toModel()

            modify(
                id,
                libraryId,
                PartialSeriesApiModel(
                    title = seriesMetadata.title,
                    authors = null,
                    books = null,
                    provider = seriesMetadata.id.provider,
                    providerID = seriesMetadata.id.itemID,
                    totalBooks = seriesMetadata.totalBooks,
                    primaryWorks = seriesMetadata.primaryWorks,
                    cover = seriesMetadata.coverURL,
                    description = seriesMetadata.description,
                ),
            )
        }

    override fun total(libraryId: UUID): Long =
        transaction {
            SeriesEntity.find { SeriesTable.library eq libraryId }.count()
        }
}
