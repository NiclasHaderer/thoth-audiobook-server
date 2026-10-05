package io.thoth.server.file.scanner

import io.thoth.server.database.tables.AuthorAgentMetadataTable
import io.thoth.server.database.tables.AuthorBookTable
import io.thoth.server.database.tables.AuthorFileMetadataTable
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.AuthorUserMetadataTable
import io.thoth.server.database.tables.BookAgentMetadataTable
import io.thoth.server.database.tables.BookFileMetadataTable
import io.thoth.server.database.tables.BookTable
import io.thoth.server.database.tables.BookUserMetadataTable
import io.thoth.server.database.tables.ImageTable
import io.thoth.server.database.tables.LibraryTable
import io.thoth.server.database.tables.SeriesAgentMetadataTable
import io.thoth.server.database.tables.SeriesBookTable
import io.thoth.server.database.tables.SeriesFileMetadataTable
import io.thoth.server.database.tables.SeriesTable
import io.thoth.server.database.tables.SeriesUserMetadataTable
import io.thoth.server.database.tables.TrackTable
import io.thoth.server.database.tables.resolvedAuthorLinks
import io.thoth.server.database.tables.resolvedSeriesLinks
import io.thoth.server.repositories.stampDeferral
import io.thoth.server.repositories.visiblyLinked
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.notInSubQuery
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.union
import java.time.Instant
import java.util.UUID

class LibraryCleanup {
    // Only ever correct straight after a completed scan: anything the walk did not stamp is treated as gone,
    // so calling this outside that window deletes tracks that were simply not visited yet.
    fun removeStaleTracks(libraryId: UUID): Unit =
        transaction {
            val scanIndex =
                LibraryTable
                    .select(LibraryTable.scanIndex)
                    .where { LibraryTable.id eq libraryId }
                    .single()[LibraryTable.scanIndex]
            TrackTable.deleteWhere {
                (TrackTable.library eq libraryId) and (TrackTable.scanIndex less scanIndex)
            }
        }

    fun removeOrphans(libraryId: UUID): Unit =
        transaction {
            val now = Instant.now()
            // Books first: deleting them cascades the link rows away, which is what leaves the authors
            // and series below without books.
            val bookHasNoTrack = BookTable.id notInSubQuery TrackTable.select(TrackTable.book)
            reap(
                table = BookTable,
                library = BookTable.library,
                deferUntil = BookTable.deferDeletionUntil,
                libraryId = libraryId,
                hidden = bookHasNoTrack,
                deletable = bookHasNoTrack,
                now = now,
            )
            reap(
                table = AuthorTable,
                library = AuthorTable.library,
                deferUntil = AuthorTable.deferDeletionUntil,
                libraryId = libraryId,
                hidden = AuthorTable.id notInSubQuery visiblyLinked(resolvedAuthorLinks, AuthorBookTable.author),
                deletable = AuthorTable.id notInSubQuery AuthorBookTable.select(AuthorBookTable.author),
                now = now,
            )
            reap(
                table = SeriesTable,
                library = SeriesTable.library,
                deferUntil = SeriesTable.deferDeletionUntil,
                libraryId = libraryId,
                hidden = SeriesTable.id notInSubQuery visiblyLinked(resolvedSeriesLinks, SeriesBookTable.series),
                deletable = SeriesTable.id notInSubQuery SeriesBookTable.select(SeriesBookTable.series),
                now = now,
            )

            removeOrphanedImages()
        }

    // One cleanup stamps the orphan with a deadline, the first cleanup past that deadline deletes it
    context(_: Transaction)
    private fun reap(
        table: Table,
        library: Column<EntityID<UUID>>,
        deferUntil: Column<Instant?>,
        libraryId: UUID,
        hidden: Op<Boolean>,
        deletable: Op<Boolean>,
        now: Instant,
    ) {
        val inLibrary = library eq libraryId
        table.deleteWhere { inLibrary and deletable and (deferUntil lessEq now) }
        stampDeferral(table, deferUntil, inLibrary, hidden, now)
    }

    fun removeOrphanedImages(): Unit =
        transaction {
            ImageTable.deleteWhere {
                ImageTable.id notInSubQuery
                    BookFileMetadataTable
                        .referenced(BookFileMetadataTable.coverId)
                        .union(BookAgentMetadataTable.referenced(BookAgentMetadataTable.coverId))
                        .union(BookUserMetadataTable.referenced(BookUserMetadataTable.coverId))
                        .union(SeriesFileMetadataTable.referenced(SeriesFileMetadataTable.coverId))
                        .union(SeriesAgentMetadataTable.referenced(SeriesAgentMetadataTable.coverId))
                        .union(SeriesUserMetadataTable.referenced(SeriesUserMetadataTable.coverId))
                        .union(AuthorFileMetadataTable.referenced(AuthorFileMetadataTable.imageId))
                        .union(AuthorAgentMetadataTable.referenced(AuthorAgentMetadataTable.imageId))
                        .union(AuthorUserMetadataTable.referenced(AuthorUserMetadataTable.imageId))
            }
        }

    private fun Table.referenced(column: Column<EntityID<UUID>?>) = select(column).where { column.isNotNull() }
}
