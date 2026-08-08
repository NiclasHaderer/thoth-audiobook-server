package io.thoth.server.repositories

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.thoth.metadata.MetadataAgent
import io.thoth.metadata.MetadataAgents
import io.thoth.models.Series
import io.thoth.models.SeriesDetailed
import io.thoth.models.SeriesUpdate
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.common.ImageDownloader
import io.thoth.server.common.extensions.escape
import io.thoth.server.common.extensions.ilike
import io.thoth.server.database.access.getOrCreateImage
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.SeriesAuthorTable
import io.thoth.server.database.tables.SeriesBookTable
import io.thoth.server.database.tables.SeriesRow
import io.thoth.server.database.tables.SeriesTable
import io.thoth.server.database.tables.addLinks
import io.thoth.server.database.tables.booksToModels
import io.thoth.server.database.tables.insert
import io.thoth.server.database.tables.replaceLinks
import io.thoth.server.database.tables.seriesToModels
import io.thoth.server.database.tables.toBookRow
import io.thoth.server.database.tables.toModel
import io.thoth.server.database.tables.toSeriesRow
import io.thoth.server.database.tables.update
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID

interface SeriesRepository : Repository<SeriesRow, Series, SeriesDetailed, SeriesUpdate> {
    fun findByName(
        seriesTitle: String,
        libraryId: UUID,
    ): SeriesRow?

    fun getOrCreate(
        seriesName: String,
        libraryId: UUID,
        authors: List<UUID>,
    ): SeriesRow

    fun create(
        seriesName: String,
        libraryId: UUID,
        authors: List<UUID>,
    ): SeriesRow
}

class SeriesRepositoryImpl :
    SeriesRepository,
    KoinComponent {
    private val authorRepository by inject<AuthorRepository>()
    private val bookRepository by inject<BookRepository>()
    private val libraryRepository by inject<LibraryRepository>()
    private val metadataAgents by inject<MetadataAgents>()
    private val imageDownloader by inject<ImageDownloader>()

    private companion object {
        val log = logger {}
    }

    override fun findByName(
        seriesTitle: String,
        libraryId: UUID,
    ): SeriesRow? =
        transaction {
            SeriesTable
                .selectAll()
                .where { titledExactly(seriesTitle) and (SeriesTable.library eq libraryId) }
                .firstOrNull()
                ?.toSeriesRow()
        }

    override fun raw(
        id: UUID,
        libraryId: UUID,
    ): SeriesRow =
        transaction {
            SeriesTable
                .selectAll()
                .where { SeriesTable.id eq id and (SeriesTable.library eq libraryId) }
                .firstOrNull()
                ?.toSeriesRow()
                ?: throw ErrorResponse.notFound("Series", id)
        }

    override fun get(
        id: UUID,
        libraryId: UUID,
    ): SeriesDetailed =
        transaction {
            val series = raw(id = id, libraryId = libraryId)

            val books =
                (SeriesBookTable innerJoin BooksTable)
                    .selectAll()
                    .where { SeriesBookTable.series eq id }
                    .orderBy(BooksTable.displayedTitle.lowerCase() to SortOrder.ASC)
                    .map { it.toBookRow() }

            SeriesDetailed.fromModel(
                series = series.toModel(),
                books = booksToModels(books),
            )
        }

    override fun getAll(
        libraryId: UUID,
        order: SortOrder,
        limit: Int,
        offset: Long,
    ): List<Series> =
        transaction {
            val rows =
                SeriesTable
                    .selectAll()
                    .where { SeriesTable.library eq libraryId }
                    .orderBy(SeriesTable.displayedTitle.lowerCase() to order)
                    .offset(offset)
                    .limit(limit)
                    .map { it.toSeriesRow() }
            seriesToModels(rows)
        }

    override fun search(
        query: String,
        libraryId: UUID,
    ): List<Series> =
        transaction {
            val rows =
                SeriesTable
                    .selectAll()
                    .where { matchesTitle(query) and (SeriesTable.library eq libraryId) }
                    .orderBy(SeriesTable.displayedTitle.lowerCase() to SortOrder.ASC)
                    .limit(searchLimit)
                    .map { it.toSeriesRow() }
            seriesToModels(rows)
        }

    override fun search(query: String): List<Series> =
        transaction {
            val rows =
                SeriesTable
                    .selectAll()
                    .where { matchesTitle(query) }
                    .orderBy(SeriesTable.displayedTitle.lowerCase() to SortOrder.ASC)
                    .limit(searchLimit)
                    .map { it.toSeriesRow() }
            seriesToModels(rows)
        }

    override fun getOrCreate(
        seriesName: String,
        libraryId: UUID,
        authors: List<UUID>,
    ): SeriesRow =
        transaction {
            val series = findByName(seriesName, libraryId)
            if (series != null) {
                SeriesAuthorTable.addLinks(SeriesAuthorTable.series, series.id, SeriesAuthorTable.author, authors)
                series
            } else {
                create(seriesName, libraryId, authors)
            }
        }

    override fun create(
        seriesName: String,
        libraryId: UUID,
        authors: List<UUID>,
    ): SeriesRow =
        transaction {
            log.info { "Created series: $seriesName" }
            val row =
                SeriesRow(
                    id = UUID.randomUUID(),
                    title = seriesName,
                    displayTitle = null,
                    totalBooks = null,
                    primaryWorks = null,
                    description = null,
                    provider = null,
                    providerID = null,
                    coverID = null,
                    library = libraryRepository.raw(libraryId).id,
                )
            SeriesTable.insert(row)
            SeriesAuthorTable.addLinks(SeriesAuthorTable.series, row.id, SeriesAuthorTable.author, authors)
            row
        }

    override fun sorting(
        libraryId: UUID,
        order: SortOrder,
        limit: Int,
        offset: Long,
    ): List<UUID> =
        transaction {
            SeriesTable
                .selectAll()
                .where { SeriesTable.library eq libraryId }
                .orderBy(SeriesTable.displayedTitle.lowerCase() to order)
                .offset(offset)
                .limit(limit)
                .map { it[SeriesTable.id].value }
        }

    override fun position(
        id: UUID,
        libraryId: UUID,
        order: SortOrder,
    ): Long =
        transaction {
            val title = raw(id, libraryId).displayedTitle.lowercase()
            SeriesTable
                .selectAll()
                .where {
                    val precedes =
                        if (order == SortOrder.ASC) {
                            SeriesTable.displayedTitle.lowerCase() less title
                        } else {
                            SeriesTable.displayedTitle.lowerCase() greater title
                        }
                    precedes and (SeriesTable.library eq libraryId)
                }.count()
        }

    override fun modify(
        id: UUID,
        libraryId: UUID,
        partial: SeriesUpdate,
    ): Series {
        val currentCover = raw(id, libraryId).coverID
        val newCover = imageDownloader.download(partial.cover?.takeUnless { it == currentCover?.toString() })
        return transaction {
            val series = raw(id, libraryId)
            val updated =
                series.copy(
                    displayTitle = partial.title ?: series.displayTitle,
                    provider = partial.provider ?: series.provider,
                    providerID = partial.providerID ?: series.providerID,
                    totalBooks = partial.totalBooks ?: series.totalBooks,
                    primaryWorks = partial.primaryWorks ?: series.primaryWorks,
                    coverID = getOrCreateImage(newCover, currentImageID = series.coverID),
                    description = partial.description ?: series.description,
                )
            SeriesTable.update(updated)

            if (partial.authors != null) {
                val authorIds = partial.authors.map { authorRepository.raw(it, libraryId).id }
                SeriesAuthorTable.replaceLinks(SeriesAuthorTable.series, id, SeriesAuthorTable.author, authorIds)
            }
            if (partial.books != null) {
                val bookIds = partial.books.map { bookRepository.raw(it, libraryId).id }
                SeriesBookTable.replaceLinks(SeriesBookTable.series, id, SeriesBookTable.book, bookIds)
            }

            updated.toModel()
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
                    series.displayedTitle,
                    library.language,
                    seriesAuthorNames(id).joinToString(", "),
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

    private fun seriesAuthorNames(seriesId: UUID): List<String> =
        (SeriesAuthorTable innerJoin AuthorTable)
            .select(AuthorTable.name, AuthorTable.displayName)
            .where { SeriesAuthorTable.series eq seriesId }
            .map { it[AuthorTable.displayName] ?: it[AuthorTable.name] }

    private data class AutoMatchQuery(
        val metadataWrapper: MetadataAgent,
        val title: String,
        val region: String,
        val authorName: String,
    )

    override fun total(libraryId: UUID): Long =
        transaction {
            SeriesTable.selectAll().where { SeriesTable.library eq libraryId }.count()
        }
}

// Same split as authors: title keeps tracking the files, displayTitle holds a rename, both stay matchable.
private fun titledExactly(title: String): Op<Boolean> = eitherTitle(escape(title))

private fun matchesTitle(query: String): Op<Boolean> = eitherTitle("%${escape(query)}%")

private fun eitherTitle(pattern: String): Op<Boolean> =
    (SeriesTable.title ilike pattern) or (SeriesTable.displayTitle ilike pattern)
