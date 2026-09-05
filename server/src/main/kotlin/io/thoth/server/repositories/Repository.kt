package io.thoth.server.repositories

import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.database.tables.AuthorBookTable
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.SeriesBookTable
import io.thoth.server.database.tables.SeriesTable
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.not
import org.jetbrains.exposed.v1.core.notInSubQuery
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID
import java.time.Duration
import java.time.Instant

// How long a book/author/series should be kept around before being properly deleted
val DEFER_DELETION_GRACE: Duration = Duration.ofHours(1)

// Hidden books do not count: an author or series whose every book is on its way out hides them
context(_: Transaction)
fun visiblyLinked(
    link: Table,
    owner: Column<EntityID<UUID>>,
) = link
    .innerJoin(BooksTable)
    .select(owner)
    .where { BooksTable.deferDeletionUntil.isNull() }

// An orphan gets a deadline, which also hides it, anything with a book again is un-hidden. An existing
// deadline is never pushed back: touching an orphan does not buy it another grace period.
context(_: Transaction)
fun stampDeferral(
    table: Table,
    deferUntil: Column<Instant?>,
    scope: Op<Boolean>,
    orphaned: Op<Boolean>,
    now: Instant,
) {
    table.update({ scope and orphaned and deferUntil.isNull() }) {
        it[deferUntil] = now.plus(DEFER_DELETION_GRACE)
    }
    table.update({ scope and not(orphaned) and deferUntil.isNotNull() }) {
        it[deferUntil] = null
    }
}

// For anything that changes which books hang off an author or series, instead of waiting for the next scan
context(_: Transaction)
fun refreshAuthorDeferral(authorIds: Collection<UUID>) =
    stampDeferral(
        table = AuthorTable,
        deferUntil = AuthorTable.deferDeletionUntil,
        scope = AuthorTable.id inList authorIds,
        orphaned = AuthorTable.id notInSubQuery visiblyLinked(AuthorBookTable, AuthorBookTable.authors),
        now = Instant.now(),
    )

context(_: Transaction)
fun refreshSeriesDeferral(seriesIds: Collection<UUID>) =
    stampDeferral(
        table = SeriesTable,
        deferUntil = SeriesTable.deferDeletionUntil,
        scope = SeriesTable.id inList seriesIds,
        orphaned = SeriesTable.id notInSubQuery visiblyLinked(SeriesBookTable, SeriesBookTable.series),
        now = Instant.now(),
    )

fun noMatch(searchedFor: String): ErrorResponse =
    ErrorResponse.missing("No metadata agent of the library had a match for '$searchedFor'")

interface Repository<RAW, NORMAL, DETAILED, PARTIAL_API> {
    val searchLimit: Int
        get() = 30

    fun raw(
        id: UUID,
        libraryId: UUID,
    ): RAW

    fun get(
        userId: UUID,
        id: UUID,
        libraryId: UUID,
    ): DETAILED

    fun getAll(
        userId: UUID,
        libraryId: UUID,
        order: SortOrder,
        limit: Int = 20,
        offset: Long = 0L,
        showInvisible: Boolean = false,
    ): List<NORMAL>

    fun search(
        userId: UUID,
        query: String,
        libraryId: UUID,
    ): List<NORMAL>

    fun search(
        userId: UUID,
        query: String,
    ): List<NORMAL>

    fun sorting(
        libraryId: UUID,
        order: SortOrder,
        limit: Int = 20,
        offset: Long = 0L,
        showInvisible: Boolean = false,
    ): List<UUID>

    fun position(
        id: UUID,
        libraryId: UUID,
        order: SortOrder,
        showInvisible: Boolean = false,
    ): Long

    fun modify(
        userId: UUID,
        id: UUID,
        libraryId: UUID,
        partial: PARTIAL_API,
    ): NORMAL

    fun autoMatch(
        userId: UUID,
        id: UUID,
        libraryId: UUID,
    ): NORMAL

    fun total(
        libraryId: UUID,
        showInvisible: Boolean = false,
    ): Long
}
