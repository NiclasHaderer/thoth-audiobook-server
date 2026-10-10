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
import io.thoth.server.database.rows.booksToModels
import io.thoth.server.database.rows.lockBookField
import io.thoth.server.database.rows.seriesToModels
import io.thoth.server.database.rows.toBookRow
import io.thoth.server.database.rows.toModel
import io.thoth.server.database.rows.toSeriesRow
import io.thoth.server.database.rows.update
import io.thoth.server.database.tables.AuthorBookTable
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.BookField
import io.thoth.server.database.tables.BookTable
import io.thoth.server.database.tables.SeriesBookTable
import io.thoth.server.database.tables.SeriesField
import io.thoth.server.database.tables.SeriesTable
import io.thoth.server.database.tables.bookIdsLinkedToSeries
import io.thoth.server.database.tables.create
import io.thoth.server.database.tables.seriesLinksWithBooks
import io.thoth.server.schedules.AutoMatchRequest
import io.thoth.server.schedules.AutoMatcher
import io.thoth.server.schedules.MatchableEntity
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
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
                books = booksToModels(linkedBooks(id), userId),
                locked = series.locked.sorted(),
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
            val id = SeriesTable.create(libraryRepository.raw(libraryId).id, seriesName, taggedName = seriesName)
            autoMatcher.matchOnCommit(AutoMatchRequest(MatchableEntity.SERIES, id, libraryId))
            raw(id, libraryId)
        }

    override fun createManual(
        seriesName: String,
        libraryId: UUID,
    ): SeriesRow =
        transaction {
            // Born an orphan: hidden and on the clock until the caller attaches books. The user named it, so a
            // later match must not rename it.
            val id =
                SeriesTable.create(
                    libraryRepository.raw(libraryId).id,
                    seriesName,
                    deferDeletionUntil = Instant.now().plus(DEFER_DELETION_GRACE),
                )
            SeriesTable.update({ SeriesTable.id eq id }) { it[locked] = setOf(SeriesField.TITLE) }
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
            val series = raw(id, libraryId)
            val edit = UserEdit(series.locked, partial.unlock)
            val cover = partial.cover.map { it?.let { getOrCreateImage(newCover, currentImageID = currentCover) } }
            SeriesTable.update(
                series.copy(
                    title = edit.value(SeriesField.TITLE, series.title, partial.title),
                    provider = edit.value(SeriesField.PROVIDER, series.provider, partial.provider),
                    providerID = edit.value(SeriesField.PROVIDER_ID, series.providerID, partial.providerID),
                    totalBooks = edit.value(SeriesField.TOTAL_BOOKS, series.totalBooks, partial.totalBooks),
                    primaryWorks = edit.value(SeriesField.PRIMARY_WORKS, series.primaryWorks, partial.primaryWorks),
                    coverID = edit.value(SeriesField.COVER_ID, series.coverID, cover),
                    description = edit.value(SeriesField.DESCRIPTION, series.description, partial.description),
                    locked = edit.locked,
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
        val current = bookIdsLinkedToSeries(seriesId).toSet()
        val removed = current - wanted
        val added = wanted - current
        SeriesBookTable.deleteWhere { (series eq seriesId) and (book inList removed) }
        added.forEach { bookId ->
            SeriesBookTable.insert {
                it[book] = bookId
                it[series] = seriesId
            }
        }
        // Dropping a book from the series locks its series, or the next scan of a file that still names it puts it
        // straight back.
        (removed + added).forEach { lockBookField(it, BookField.SERIES) }
    }

    context(_: Transaction)
    private fun linkedBooks(seriesId: UUID): List<BookRow> =
        seriesLinksWithBooks
            .selectAll()
            .where { (SeriesBookTable.series eq seriesId) and BookTable.visible }
            .orderBy(BookTable.title to SortOrder.ASC)
            .map { it.toBookRow() }

    override fun autoMatch(
        userId: UUID,
        id: UUID,
        libraryId: UUID,
    ): Series {
        val (metadataWrapper, title, region, authorName, language, preferFile) =
            transaction {
                val series = raw(id, libraryId)
                val library = libraryRepository.raw(libraryId)
                AutoMatchQuery(
                    metadataAgents.forLibrary(library),
                    series.title,
                    library.region,
                    seriesAuthorNames(id).joinToString(", "),
                    library.language,
                    library.preferEmbeddedMetadata,
                )
            }

        val seriesMetadata =
            runBlocking {
                metadataWrapper.bestSeriesMatch(title, region, authorName, language)
            } ?: throw noMatch(title)

        val newCover = imageDownloader.download(seriesMetadata.coverURL)
        return transaction {
            val series = raw(id, libraryId)
            val write = AutomaticWrite(series.locked, onlyFillEmpty = preferFile)
            SeriesTable.update(
                series.copy(
                    title = write.value(SeriesField.TITLE, series.title) { seriesMetadata.title },
                    provider = write.overwrite(SeriesField.PROVIDER, series.provider) { seriesMetadata.id.provider },
                    providerID =
                        write.overwrite(SeriesField.PROVIDER_ID, series.providerID) { seriesMetadata.id.itemID },
                    totalBooks = write.value(SeriesField.TOTAL_BOOKS, series.totalBooks) { seriesMetadata.totalBooks },
                    primaryWorks =
                        write.value(SeriesField.PRIMARY_WORKS, series.primaryWorks) { seriesMetadata.primaryWorks },
                    description =
                        write.value(SeriesField.DESCRIPTION, series.description) { seriesMetadata.description },
                    coverID = write.value(SeriesField.COVER_ID, series.coverID) { getOrCreateImage(newCover, it) },
                ),
            )
            raw(id, libraryId).toModel()
        }
    }

    private fun seriesAuthorNames(seriesId: UUID): List<String> =
        SeriesBookTable
            .join(AuthorBookTable, JoinType.INNER, SeriesBookTable.book, AuthorBookTable.book)
            .join(AuthorTable, JoinType.INNER, AuthorBookTable.author, AuthorTable.id)
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
        val preferFile: Boolean,
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
    SeriesTable
        .select(SeriesTable.id)
        .where {
            ((SeriesTable.title ilike pattern) or (SeriesTable.taggedName ilike pattern)) and
                (SeriesTable.library eq libraryId)
        }.firstOrNull()
        ?.get(SeriesTable.id)
        ?.value

private fun matchesTitle(query: String): Op<Boolean> = SeriesTable.title ilike "%${escape(query)}%"
