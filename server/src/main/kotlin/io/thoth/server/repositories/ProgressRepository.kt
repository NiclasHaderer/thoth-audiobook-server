package io.thoth.server.repositories

import io.thoth.models.Book
import io.thoth.models.ListeningHistoryEntry
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.config.ThothConfig
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.TracksTable
import io.thoth.server.database.tables.ProgressLogRow
import io.thoth.server.database.tables.ProgressLogTable
import io.thoth.server.database.tables.UserBookProgressRow
import io.thoth.server.database.tables.UserBookProgressTable
import io.thoth.server.database.tables.insert
import io.thoth.server.database.tables.toProgressLogRow
import io.thoth.server.database.tables.toUserBookProgressRow
import io.thoth.server.database.tables.update
import io.thoth.server.database.views.BookMetadataView
import io.thoth.server.database.views.booksToModels
import io.thoth.server.database.views.toBookRow
import io.thoth.server.plugins.auth.resolveUserPermissions
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration
import java.time.Instant
import java.util.UUID

/** Once the position is this close to the end, the book counts as listened. */
private const val FINISHED_THRESHOLD_MS = 30_000L

private val WEEK: Duration = Duration.ofDays(7)

interface ProgressRepository {
    fun upsert(
        userId: UUID,
        libraryId: UUID,
        bookId: UUID,
        positionMs: Long,
    )

    fun setFinished(
        userId: UUID,
        libraryId: UUID,
        bookId: UUID,
        finished: Boolean,
    )

    fun setDismissed(
        userId: UUID,
        libraryId: UUID,
        bookId: UUID,
        dismissed: Boolean,
    )

    fun continueListening(
        userId: UUID,
        limit: Int,
    ): List<Book>

    fun history(
        userId: UUID,
        limit: Int,
        offset: Long,
    ): List<ListeningHistoryEntry>

    fun historyTotal(userId: UUID): Long
}

class ProgressRepositoryImpl :
    ProgressRepository,
    KoinComponent {
    private val config by inject<ThothConfig>()

    override fun upsert(
        userId: UUID,
        libraryId: UUID,
        bookId: UUID,
        positionMs: Long,
    ) {
        transaction {
            if (positionMs < 0) throw ErrorResponse.userError("positionMs must not be negative")
            val durationMs = durationOfBookIn(libraryId, bookId)
            val now = Instant.now()
            val existing = row(userId, bookId)
            val finished = durationMs > 0 && positionMs >= durationMs - FINISHED_THRESHOLD_MS

            val updated =
                UserBookProgressRow(
                    user = userId,
                    book = bookId,
                    library = libraryId,
                    positionMs = positionMs,
                    updatedAt = now,
                    // Seeking back out of the finished window means the book is being re-listened to
                    finishedAt = if (finished) existing?.finishedAt ?: now else null,
                    // Playing a dismissed book undoes the dismissal - it belongs back in continue listening
                    dismissedAt = null,
                )
            write(existing, updated)
            logProgress(updated, now)
        }
    }

    override fun setFinished(
        userId: UUID,
        libraryId: UUID,
        bookId: UUID,
        finished: Boolean,
    ) {
        transaction {
            durationOfBookIn(libraryId, bookId)
            val now = Instant.now()
            val existing = row(userId, bookId)
            val updated =
                UserBookProgressRow(
                    user = userId,
                    book = bookId,
                    library = libraryId,
                    // Marking a book is an annotation, not a seek. Overwriting the position here would
                    // throw away the listener's place, and un-marking could never give it back.
                    positionMs = existing?.positionMs ?: 0,
                    updatedAt = now,
                    finishedAt = if (finished) now else null,
                    // Un-finishing says "I want to keep going", so it belongs back in continue listening
                    dismissedAt = if (finished) existing?.dismissedAt else null,
                )
            write(existing, updated)
        }
    }

    override fun setDismissed(
        userId: UUID,
        libraryId: UUID,
        bookId: UUID,
        dismissed: Boolean,
    ) {
        transaction {
            durationOfBookIn(libraryId, bookId)
            val now = Instant.now()
            val existing = row(userId, bookId) ?: throw ErrorResponse.notFound("Progress", bookId)
            val updated = existing.copy(updatedAt = now, dismissedAt = if (dismissed) now else null)
            write(existing, updated)
        }
    }

    override fun continueListening(
        userId: UUID,
        limit: Int,
    ): List<Book> =
        transaction {
            val cutoff = Instant.now().minus(WEEK.multipliedBy(config.continueListeningWeeks.toLong()))
            val readable = readableLibraries(userId)
            val rows =
                UserBookProgressTable
                    .selectAll()
                    .where {
                        (UserBookProgressTable.user eq userId) and
                            (UserBookProgressTable.library inList readable) and
                            UserBookProgressTable.finishedAt.isNull() and
                            UserBookProgressTable.dismissedAt.isNull() and
                            (UserBookProgressTable.positionMs greater 0L) and
                            (UserBookProgressTable.updatedAt greaterEq cutoff)
                    }.orderBy(UserBookProgressTable.updatedAt to SortOrder.DESC)
                    .limit(limit)
                    .map { it.toUserBookProgressRow() }

            val books = booksById(rows.map { it.book }, userId, readable)
            rows.mapNotNull { books[it.book] }
        }

    override fun history(
        userId: UUID,
        limit: Int,
        offset: Long,
    ): List<ListeningHistoryEntry> =
        transaction {
            val readable = readableLibraries(userId)
            val rows =
                ProgressLogTable
                    .selectAll()
                    .where {
                        (ProgressLogTable.user eq userId) and
                            (ProgressLogTable.library inList readable)
                    }.orderBy(ProgressLogTable.at to SortOrder.DESC)
                    .offset(offset)
                    .limit(limit)
                    .map { it.toProgressLogRow() }

            val books = booksById(rows.map { it.book }, userId, readable)
            rows.mapNotNull { row ->
                val book = books[row.book] ?: return@mapNotNull null
                ListeningHistoryEntry(
                    id = row.id,
                    book = book,
                    positionMs = row.positionMs,
                    at = row.at,
                )
            }
        }

    override fun historyTotal(userId: UUID): Long =
        transaction {
            ProgressLogTable
                .selectAll()
                .where {
                    (ProgressLogTable.user eq userId) and
                        (ProgressLogTable.library inList readableLibraries(userId))
                }.count()
        }

    context(_: Transaction)
    private fun logProgress(
        progress: UserBookProgressRow,
        now: Instant,
    ) {
        ProgressLogTable.insert(
            ProgressLogRow(
                id = UUID.randomUUID(),
                user = progress.user,
                book = progress.book,
                library = progress.library,
                positionMs = progress.positionMs,
                at = now,
            ),
        )
    }

    context(_: Transaction)
    private fun write(
        existing: UserBookProgressRow?,
        updated: UserBookProgressRow,
    ) {
        if (existing == null) UserBookProgressTable.insert(updated) else UserBookProgressTable.update(updated)
    }

    context(_: Transaction)
    private fun booksById(
        bookIds: List<UUID>,
        userId: UUID,
        readable: List<UUID>,
    ): Map<UUID, Book> {
        // The progress log holds one row per write, so a page of history is mostly the same few books
        // repeated. Deduplicating keeps the IN list proportional to the books, not to the rows.
        val ids = bookIds.distinct()
        if (ids.isEmpty()) return emptyMap()
        val rows =
            BookMetadataView
                .selectAll()
                .where { (BookMetadataView.id inList ids) and (BookMetadataView.library inList readable) }
                .map { it.toBookRow() }
        return booksToModels(rows, userId).associateBy { it.id }
    }

    context(_: Transaction)
    private fun row(
        userId: UUID,
        bookId: UUID,
    ): UserBookProgressRow? =
        UserBookProgressTable
            .selectAll()
            .where { (UserBookProgressTable.user eq userId) and (UserBookProgressTable.book eq bookId) }
            .firstOrNull()
            ?.toUserBookProgressRow()

    context(_: Transaction)
    private fun durationOfBookIn(
        libraryId: UUID,
        bookId: UUID,
    ): Long {
        val total = TracksTable.durationMs.sum()
        val row =
            (BooksTable leftJoin TracksTable)
                .select(BooksTable.id, total)
                .where { (BooksTable.id eq bookId) and (BooksTable.library eq libraryId) }
                .groupBy(BooksTable.id)
                .firstOrNull() ?: throw ErrorResponse.notFound("Book", bookId)
        // Can be null if the book has no tracks
        return row[total] ?: 0
    }

    private fun readableLibraries(userId: UUID): List<UUID> =
        resolveUserPermissions(userId).libraries.map { it.id }
}
