package io.thoth.server.repositories

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.thoth.metadata.MetadataAgent
import io.thoth.metadata.MetadataAgents
import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.metadata.responses.MetadataRegion
import io.thoth.models.Series
import io.thoth.models.SeriesDetailed
import io.thoth.models.SeriesUpdate
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.common.ImageDownloader
import io.thoth.server.common.extensions.escape
import io.thoth.server.common.extensions.ilike
import io.thoth.server.database.access.getOrCreateImage
import io.thoth.server.database.tables.BookUserMetadataTable
import io.thoth.server.database.tables.SeriesAgentMetadataTable
import io.thoth.server.database.tables.MetadataLayer
import io.thoth.server.database.tables.SeriesFileMetadataTable
import io.thoth.server.database.tables.SeriesMetadata
import io.thoth.server.database.tables.SeriesMetadataRow
import io.thoth.server.database.views.SeriesRow
import io.thoth.server.database.tables.SeriesTable
import io.thoth.server.database.tables.SeriesUserMetadataTable
import io.thoth.server.database.tables.bookIdsLinkedToSeries
import io.thoth.server.database.views.bookSeries
import io.thoth.server.database.views.booksToModels
import io.thoth.server.database.tables.create
import io.thoth.server.database.tables.layer
import io.thoth.server.database.tables.replaceBookSeries
import io.thoth.server.database.views.seriesToModels
import io.thoth.server.database.views.toBookRow
import io.thoth.server.database.views.toModel
import io.thoth.server.database.views.toSeriesRow
import io.thoth.server.database.tables.write
import io.thoth.server.database.views.AuthorMetadataView
import io.thoth.server.database.views.BookMetadataView
import io.thoth.server.database.views.BookAuthorView
import io.thoth.server.database.views.BookSeriesView
import io.thoth.server.database.views.BookRow
import io.thoth.server.database.views.SeriesMetadataView
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID
import java.time.Instant

interface SeriesRepository : Repository<SeriesRow, Series, SeriesDetailed, SeriesUpdate> {
    fun findByTaggedName(
        seriesTitle: String,
        libraryId: UUID,
    ): SeriesRow?

    fun getOrCreate(
        seriesName: String,
        libraryId: UUID,
    ): SeriesRow

    fun create(
        seriesName: String,
        libraryId: UUID,
    ): SeriesRow

    fun createManual(
        seriesName: String,
        libraryId: UUID,
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

    override fun findByTaggedName(
        seriesTitle: String,
        libraryId: UUID,
    ): SeriesRow? =
        transaction {
            idOfTaggedName(escape(seriesTitle), libraryId)?.let { raw(it, libraryId) }
        }

    override fun raw(
        id: UUID,
        libraryId: UUID,
    ): SeriesRow =
        transaction {
            SeriesMetadataView
                .selectAll()
                .where { SeriesMetadataView.id eq id and (SeriesMetadataView.library eq libraryId) }
                .firstOrNull()
                ?.toSeriesRow()
                ?: throw ErrorResponse.notFound("Series", id)
        }

    override fun get(
        userId: UUID,
        id: UUID,
        libraryId: UUID,
    ): SeriesDetailed =
        transaction {
            val series = raw(id = id, libraryId = libraryId)

            SeriesDetailed.fromModel(
                series = series.toModel(),
                books = booksToModels(resolvedBooks(id), userId),
            )
        }

    override fun getAll(
        userId: UUID,
        libraryId: UUID,
        order: SortOrder,
        limit: Int,
        offset: Long,
    ): List<Series> =
        transaction {
            val rows =
                SeriesMetadataView
                    .selectAll()
                    .where { SeriesMetadataView.library eq libraryId }
                    .orderBy(SeriesMetadataView.title.lowerCase() to order)
                    .offset(offset)
                    .limit(limit)
                    .map { it.toSeriesRow() }
            seriesToModels(rows)
        }

    override fun search(
        userId: UUID,
        query: String,
        libraryId: UUID,
    ): List<Series> =
        transaction {
            val rows =
                SeriesMetadataView
                    .selectAll()
                    .where { matchesTitle(query) and (SeriesMetadataView.library eq libraryId) }
                    .orderBy(SeriesMetadataView.title.lowerCase() to SortOrder.ASC)
                    .limit(searchLimit)
                    .map { it.toSeriesRow() }
            seriesToModels(rows)
        }

    override fun search(
        userId: UUID,
        query: String,
    ): List<Series> =
        transaction {
            val rows =
                SeriesMetadataView
                    .selectAll()
                    .where { matchesTitle(query) }
                    .orderBy(SeriesMetadataView.title.lowerCase() to SortOrder.ASC)
                    .limit(searchLimit)
                    .map { it.toSeriesRow() }
            seriesToModels(rows)
        }

    override fun getOrCreate(
        seriesName: String,
        libraryId: UUID,
    ): SeriesRow = transaction { findByTaggedName(seriesName, libraryId) ?: create(seriesName, libraryId) }

    override fun create(
        seriesName: String,
        libraryId: UUID,
    ): SeriesRow =
        transaction {
            log.info { "Created series: $seriesName" }
            val id = SeriesTable.create(libraryRepository.raw(libraryId).id)
            SeriesFileMetadataTable.write(SeriesMetadataRow(series = id, title = seriesName))
            raw(id, libraryId)
        }

    override fun createManual(
        seriesName: String,
        libraryId: UUID,
    ): SeriesRow =
        transaction {
            val id =
                SeriesTable.create(
                    libraryRepository.raw(libraryId).id,
                    deferDeletionUntil = Instant.now().plus(DEFER_DELETION_GRACE),
                )
            SeriesUserMetadataTable.write(SeriesMetadataRow(series = id, title = seriesName))
            raw(id, libraryId)
        }

    override fun sorting(
        libraryId: UUID,
        order: SortOrder,
        limit: Int,
        offset: Long,
    ): List<UUID> =
        transaction {
            SeriesMetadataView
                .selectAll()
                .where { SeriesMetadataView.library eq libraryId }
                .orderBy(SeriesMetadataView.title.lowerCase() to order)
                .offset(offset)
                .limit(limit)
                .map { it[SeriesMetadataView.id] }
        }

    override fun position(
        id: UUID,
        libraryId: UUID,
        order: SortOrder,
    ): Long =
        transaction {
            val title = raw(id, libraryId).title.lowercase()
            SeriesMetadataView
                .selectAll()
                .where {
                    val precedes =
                        if (order == SortOrder.ASC) {
                            SeriesMetadataView.title.lowerCase() less title
                        } else {
                            SeriesMetadataView.title.lowerCase() greater title
                        }
                    precedes and (SeriesMetadataView.library eq libraryId)
                }.count()
        }

    override fun modify(
        userId: UUID,
        id: UUID,
        libraryId: UUID,
        partial: SeriesUpdate,
    ): Series {
        val currentCover = raw(id, libraryId).coverID
        val newCover = imageDownloader.download(partial.cover?.takeUnless { it == currentCover?.toString() })
        return transaction {
            val user = SeriesUserMetadataTable.layer(id)
            SeriesUserMetadataTable.write(
                user.copy(
                    title = partial.title ?: user.title,
                    provider = partial.provider ?: user.provider,
                    providerID = partial.providerID ?: user.providerID,
                    totalBooks = partial.totalBooks ?: user.totalBooks,
                    primaryWorks = partial.primaryWorks ?: user.primaryWorks,
                    coverID = getOrCreateImage(newCover, currentImageID = user.coverID),
                    description = partial.description ?: user.description,
                ),
            )

            if (partial.books != null) {
                setBooks(id, partial.books.map { bookRepository.raw(it, libraryId).id }.toSet())
            }

            SeriesTable.update({ SeriesTable.id eq id }) {
                it[deferDeletionUntil] = Instant.now().plus(DEFER_DELETION_GRACE)
            }
            raw(id, libraryId).toModel()
        }
    }

    context(_: Transaction)
    private fun setBooks(
        seriesId: UUID,
        wanted: Set<UUID>,
    ) {
        // Every layer counts here, not just the winning one: dropping a book from the series has to write a
        // user layer for a book whose file tags still name it, or the next scan puts it straight back.
        val affected = (bookIdsLinkedToSeries(seriesId) + wanted).distinct()
        // Read the current membership before writing, so each book keeps the series it already resolved to
        val resolved = bookSeries(affected)
        affected.forEach { bookId ->
            val current = resolved[bookId].orEmpty().map { it.id }.toSet()
            val next = if (bookId in wanted) current + seriesId else current - seriesId
            if (next == current) return@forEach
            replaceBookSeries(bookId, MetadataLayer.USER, next.associateWith { null })
            BookUserMetadataTable.write(BookUserMetadataTable.layer(bookId).copy(seriesSet = true))
        }
    }

    context(_: Transaction)
    private fun resolvedBooks(seriesId: UUID): List<BookRow> =
        BookMetadataView
            .join(BookSeriesView, JoinType.INNER, BookMetadataView.id, BookSeriesView.book)
            .selectAll()
            .where { BookSeriesView.series eq seriesId }
            .orderBy(BookMetadataView.title.lowerCase() to SortOrder.ASC)
            .map { it.toBookRow() }

    override fun autoMatch(
        userId: UUID,
        id: UUID,
        libraryId: UUID,
    ): Series {
        val (metadataWrapper, title, region, authorName, language) =
            transaction {
                val series = raw(id, libraryId)
                val library = libraryRepository.raw(libraryId)
                AutoMatchQuery(
                    metadataAgents.forLibrary(library),
                    series.title,
                    library.region,
                    seriesAuthorNames(id).joinToString(", "),
                    library.language,
                )
            }

        val seriesMetadata =
            runBlocking {
                metadataWrapper.getSeriesByName(title, region, authorName, language).firstOrNull()
            } ?: return transaction { raw(id, libraryId).toModel() }

        val newCover = imageDownloader.download(seriesMetadata.coverURL)
        return transaction {
            val agent = SeriesAgentMetadataTable.layer(id)
            SeriesAgentMetadataTable.write(
                agent.copy(
                    title = seriesMetadata.title ?: agent.title,
                    provider = seriesMetadata.id.provider,
                    providerID = seriesMetadata.id.itemID,
                    totalBooks = seriesMetadata.totalBooks ?: agent.totalBooks,
                    primaryWorks = seriesMetadata.primaryWorks ?: agent.primaryWorks,
                    description = seriesMetadata.description ?: agent.description,
                    coverID = getOrCreateImage(newCover, currentImageID = agent.coverID),
                ),
            )
            raw(id, libraryId).toModel()
        }
    }

    private fun seriesAuthorNames(seriesId: UUID): List<String> =
        BookSeriesView
            .join(BookAuthorView, JoinType.INNER, BookSeriesView.book, BookAuthorView.book)
            .join(AuthorMetadataView, JoinType.INNER, BookAuthorView.author, AuthorMetadataView.id)
            .select(AuthorMetadataView.name)
            .where { BookSeriesView.series eq seriesId }
            .map { it[AuthorMetadataView.name] }
            .distinct()

    private data class AutoMatchQuery(
        val metadataWrapper: MetadataAgent,
        val title: String,
        val region: MetadataRegion,
        val authorName: String,
        val language: MetadataLanguage,
    )

    override fun total(libraryId: UUID): Long =
        transaction {
            SeriesTable.selectAll().where { SeriesTable.library eq libraryId }.count()
        }
}

context(_: Transaction)
private fun idOfTaggedName(
    pattern: String,
    libraryId: UUID,
): UUID? =
    idInLayer(SeriesFileMetadataTable, pattern, libraryId)
        ?: idInLayer(SeriesUserMetadataTable, pattern, libraryId)
        ?: idInLayer(SeriesAgentMetadataTable, pattern, libraryId)

context(_: Transaction)
private fun idInLayer(
    table: SeriesMetadata,
    pattern: String,
    libraryId: UUID,
): UUID? =
    (SeriesTable innerJoin table)
        .select(SeriesTable.id)
        .where { (table.title ilike pattern) and (SeriesTable.library eq libraryId) }
        .firstOrNull()
        ?.get(SeriesTable.id)
        ?.value

private fun matchesTitle(query: String): Op<Boolean> =
    SeriesMetadataView.title ilike "%${escape(query)}%"
