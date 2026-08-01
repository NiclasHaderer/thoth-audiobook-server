package io.thoth.server.repositories

import io.thoth.metadata.MetadataAgent
import io.thoth.metadata.MetadataAgents
import io.thoth.models.Series
import io.thoth.models.SeriesDetailed
import io.thoth.models.SeriesUpdate
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.common.extensions.escape
import io.thoth.server.common.extensions.ilike
import io.thoth.server.common.extensions.toSizedIterable
import io.thoth.server.database.access.fetchImage
import io.thoth.server.database.access.getNewImage
import io.thoth.server.database.tables.AuthorEntity
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.ImageEntity
import io.thoth.server.database.tables.SeriesEntity
import io.thoth.server.database.tables.SeriesTable
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.SizedCollection
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID

interface SeriesRepository : Repository<SeriesEntity, Series, SeriesDetailed, SeriesUpdate> {
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
    private val metadataAgents by inject<MetadataAgents>()

    private companion object {
        val log = logger {}
    }

    override fun findByName(
        seriesTitle: String,
        libraryId: UUID,
    ): SeriesEntity? =
        transaction {
            SeriesEntity
                .find { (SeriesTable.title ilike escape(seriesTitle)) and (SeriesTable.library eq libraryId) }
                .firstOrNull()
        }

    override fun raw(
        id: UUID,
        libraryId: UUID,
    ): SeriesEntity =
        transaction {
            SeriesEntity.find { SeriesTable.id eq id and (SeriesTable.library eq libraryId) }.firstOrNull()
                ?: throw ErrorResponse.notFound("Series", id)
        }

    override fun get(
        id: UUID,
        libraryId: UUID,
    ): SeriesDetailed =
        transaction {
            val series = raw(id = id, libraryId = libraryId)

            SeriesDetailed.fromModel(
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
                .find { (SeriesTable.title ilike "%${escape(query)}%") and (SeriesTable.library eq libraryId) }
                .orderBy(SeriesTable.title.lowerCase() to SortOrder.ASC)
                .limit(searchLimit)
                .map { it.toModel() }
        }

    override fun search(query: String): List<Series> =
        transaction {
            SeriesEntity
                .find { SeriesTable.title ilike "%${escape(query)}%" }
                .orderBy(SeriesTable.title.lowerCase() to SortOrder.ASC)
                .limit(searchLimit)
                .map { it.toModel() }
        }

    override fun getOrCreate(
        seriesName: String,
        libraryId: UUID,
        dbAuthor: List<AuthorEntity>,
    ): SeriesEntity =
        transaction {
            val series = findByName(seriesName, libraryId)
            if (series != null) {
                series.authors = SizedCollection((series.authors.toList() + dbAuthor).distinctBy { it.id })
                series
            } else {
                create(seriesName, libraryId, dbAuthor)
            }
        }

    override fun create(
        seriesName: String,
        libraryId: UUID,
        dbAuthor: List<AuthorEntity>,
    ): SeriesEntity =
        transaction {
            log.info { "Created series: $seriesName" }
            SeriesEntity.new {
                title = seriesName
                authors = SizedCollection(dbAuthor.distinctBy { it.id })
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
                .takeIf { it >= 0 }
                ?.toLong()
                ?: throw ErrorResponse.notFound("Series", id)
        }

    override fun modify(
        id: UUID,
        libraryId: UUID,
        partial: SeriesUpdate,
    ): Series {
        val newCover = fetchImage(partial.cover)
        return transaction {
            val series = raw(id, libraryId)

            series.apply {
                title = partial.title ?: title
                provider = partial.provider ?: provider
                providerID = partial.providerID ?: providerID
                totalBooks = partial.totalBooks ?: totalBooks
                primaryWorks = partial.primaryWorks ?: primaryWorks
                coverID = ImageEntity.getNewImage(newCover, currentImageID = coverID, default = coverID)
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
    }

    override fun autoMatch(
        id: UUID,
        libraryId: UUID,
    ): Series {
        val (metadataWrapper, title, region, authorName) =
            transaction {
                val series = raw(id, libraryId)
                val library = libraryRepository.raw(libraryId)
                AutoMatchQuery(
                    metadataAgents.forLibrary(library),
                    series.title,
                    library.language,
                    series.authors.joinToString(", ") { it.name },
                )
            }

        val seriesMetadata =
            runBlocking {
                metadataWrapper.getSeriesByName(title, region, authorName).firstOrNull()
            } ?: return transaction { raw(id, libraryId).toModel() }

        return modify(
            id,
            libraryId,
            SeriesUpdate(
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

    private data class AutoMatchQuery(
        val metadataWrapper: MetadataAgent,
        val title: String,
        val region: String,
        val authorName: String,
    )

    override fun total(libraryId: UUID): Long =
        transaction {
            SeriesEntity.find { SeriesTable.library eq libraryId }.count()
        }
}
