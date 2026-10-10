package io.thoth.server.repositories

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.thoth.metadata.MetadataAgentWrapper
import io.thoth.metadata.MetadataAgents
import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.metadata.responses.MetadataRegion
import io.thoth.models.Series
import io.thoth.models.SeriesDetailed
import io.thoth.models.SeriesUpdate
import io.thoth.openapi.common.ifSet
import io.thoth.openapi.common.map
import io.thoth.openapi.common.orElse
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.common.ImageDownloader
import io.thoth.server.common.exposed.unless
import io.thoth.server.common.extensions.escape
import io.thoth.server.common.extensions.ilike
import io.thoth.server.database.access.getOrCreateImage
import io.thoth.server.database.rows.BookRow
import io.thoth.server.database.rows.SeriesRow
import io.thoth.server.database.rows.bookSeries
import io.thoth.server.database.rows.booksToModels
import io.thoth.server.database.rows.seriesToModels
import io.thoth.server.database.rows.toBookRow
import io.thoth.server.database.rows.toModel
import io.thoth.server.database.rows.toSeriesRow
import io.thoth.server.database.tables.AuthorBookTable
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.BookField
import io.thoth.server.database.tables.BookTable
import io.thoth.server.database.tables.BookUserMetadataTable
import io.thoth.server.database.tables.MetadataLayer
import io.thoth.server.database.tables.SeriesAgentMetadataTable
import io.thoth.server.database.tables.SeriesBookTable
import io.thoth.server.database.tables.SeriesField
import io.thoth.server.database.tables.SeriesFileMetadataTable
import io.thoth.server.database.tables.SeriesMetadata
import io.thoth.server.database.tables.SeriesMetadataRow
import io.thoth.server.database.tables.SeriesTable
import io.thoth.server.database.tables.SeriesUserMetadataTable
import io.thoth.server.database.tables.bookIdsLinkedToSeries
import io.thoth.server.database.tables.create
import io.thoth.server.database.tables.layer
import io.thoth.server.database.tables.replaceBookSeries
import io.thoth.server.database.tables.resolvedSeriesLinks
import io.thoth.server.database.tables.write
import io.thoth.server.schedules.AutoMatchRequest
import io.thoth.server.schedules.AutoMatcher
import io.thoth.server.schedules.MatchableEntity
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.util.UUID

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
    private val autoMatcher by inject<AutoMatcher>()

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
            SeriesTable
                .selectAll()
                .where { SeriesTable.id eq id and (SeriesTable.library eq libraryId) }
                .firstOrNull()
                ?.toSeriesRow()
                ?: throw ErrorResponse.notFound("Series", id)
        }

    override fun get(
        userId: UUID,
        id: UUID,
        libraryId: UUID,
        showInvisible: Boolean,
    ): SeriesDetailed =
        transaction {
            val series = raw(id = id, libraryId = libraryId)
            if (!showInvisible) requireVisible(SeriesTable, "Series", id)

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
        showInvisible: Boolean,
    ): List<Series> =
        transaction {
            val rows =
                SeriesTable
                    .selectAll()
                    .where {
                        (SeriesTable.library eq libraryId) and SeriesTable.visible.unless(showInvisible)
                    }.orderBy(SeriesTable.title to order)
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
                SeriesTable
                    .selectAll()
                    .where {
                        matchesTitle(query) and (SeriesTable.library eq libraryId) and
                            SeriesTable.visible
                    }.orderBy(SeriesTable.title to SortOrder.ASC)
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
            val id = SeriesTable.create(libraryRepository.raw(libraryId).id, seriesName)
            SeriesFileMetadataTable.write(SeriesMetadataRow(series = id, title = seriesName))
            autoMatcher.matchOnCommit(AutoMatchRequest(MatchableEntity.SERIES, id, libraryId))
            raw(id, libraryId)
        }

    override fun createManual(
        seriesName: String,
        libraryId: UUID,
    ): SeriesRow =
        transaction {
            // Born an orphan: hidden and on the clock until the caller attaches books
            val id =
                SeriesTable.create(
                    libraryRepository.raw(libraryId).id,
                    seriesName,
                    deferDeletionUntil = Instant.now().plus(DEFER_DELETION_GRACE),
                )
            SeriesUserMetadataTable.write(SeriesMetadataRow(series = id, title = seriesName))
            raw(id, libraryId)
        }

    override fun modify(
        userId: UUID,
        id: UUID,
        libraryId: UUID,
        partial: SeriesUpdate,
    ): Series {
        val currentCover = raw(id, libraryId).coverID
        val newCover =
            imageDownloader.download(partial.cover.orElse(null)?.takeUnless { it == currentCover?.toString() })
        return transaction {
            val user = SeriesUserMetadataTable.layer(id)
            val edit = LayerEdit(user.claimed)
            SeriesUserMetadataTable.write(
                user.copy(
                    title = edit.value(SeriesField.TITLE, partial.title, user.title),
                    provider = edit.value(SeriesField.PROVIDER, partial.provider, user.provider),
                    providerID = edit.value(SeriesField.PROVIDER_ID, partial.providerID, user.providerID),
                    totalBooks = edit.value(SeriesField.TOTAL_BOOKS, partial.totalBooks, user.totalBooks),
                    primaryWorks = edit.value(SeriesField.PRIMARY_WORKS, partial.primaryWorks, user.primaryWorks),
                    coverID =
                        edit.value(
                            SeriesField.COVER_ID,
                            partial.cover.map { it?.let { getOrCreateImage(newCover, currentImageID = currentCover) } },
                            user.coverID,
                        ),
                    description = edit.value(SeriesField.DESCRIPTION, partial.description, user.description),
                    claimed = edit.claimed,
                ),
            )

            partial.books.ifSet { books ->
                setBooks(id, books.map { bookRepository.raw(it, libraryId).id }.toSet())
            }

            refreshSeriesDeferral(listOf(id))
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
            val layer = BookUserMetadataTable.layer(bookId)
            BookUserMetadataTable.write(layer.copy(claimed = layer.claimed + BookField.SERIES))
        }
    }

    context(_: Transaction)
    private fun resolvedBooks(seriesId: UUID): List<BookRow> =
        resolvedSeriesLinks
            .selectAll()
            .where { (SeriesBookTable.series eq seriesId) and BookTable.visible }
            .orderBy(BookTable.title to SortOrder.ASC)
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
                metadataWrapper.bestSeriesMatch(title, region, authorName, language)
            } ?: throw noMatch(title)

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
        resolvedSeriesLinks
            .join(AuthorBookTable, JoinType.INNER, SeriesBookTable.book, AuthorBookTable.book) {
                AuthorBookTable.addedBy eq BookTable.authorsFrom
            }.join(AuthorTable, JoinType.INNER, AuthorBookTable.author, AuthorTable.id)
            .select(AuthorTable.name)
            .where { SeriesBookTable.series eq seriesId }
            .map { it[AuthorTable.name] }
            .distinct()

    private data class AutoMatchQuery(
        val metadataWrapper: MetadataAgentWrapper,
        val title: String,
        val region: MetadataRegion,
        val authorName: String,
        val language: MetadataLanguage,
    )

    override fun total(
        libraryId: UUID,
        showInvisible: Boolean,
    ): Long =
        transaction {
            SeriesTable
                .selectAll()
                .where {
                    (SeriesTable.library eq libraryId) and SeriesTable.deferDeletionUntil.isNull().unless(showInvisible)
                }.count()
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

private fun matchesTitle(query: String): Op<Boolean> = SeriesTable.title ilike "%${escape(query)}%"
