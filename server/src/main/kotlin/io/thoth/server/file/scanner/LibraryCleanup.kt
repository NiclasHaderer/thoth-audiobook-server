package io.thoth.server.file.scanner

import io.thoth.server.database.tables.AuthorAgentMetadataTable
import io.thoth.server.database.tables.AuthorBookTable
import io.thoth.server.database.tables.AuthorFileMetadataTable
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.AuthorUserMetadataTable
import io.thoth.server.database.tables.BookAgentMetadataTable
import io.thoth.server.database.tables.BookFileMetadataTable
import io.thoth.server.database.tables.BookUserMetadataTable
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.ImageTable
import io.thoth.server.database.tables.LibrariesTable
import io.thoth.server.database.tables.SeriesAgentMetadataTable
import io.thoth.server.database.tables.SeriesBookTable
import io.thoth.server.database.tables.SeriesFileMetadataTable
import io.thoth.server.database.tables.SeriesTable
import io.thoth.server.database.tables.SeriesUserMetadataTable
import io.thoth.server.database.tables.TracksTable
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
import java.util.UUID
import java.time.Instant

class LibraryCleanup {
    // Only ever correct straight after a completed scan: anything the walk did not stamp is treated as gone,
    // so calling this outside that window deletes tracks that were simply not visited yet.
    fun removeStaleTracks(libraryId: UUID): Unit =
        transaction {
            val scanIndex =
                LibrariesTable
                    .select(LibrariesTable.scanIndex)
                    .where { LibrariesTable.id eq libraryId }
                    .single()[LibrariesTable.scanIndex]
            TracksTable.deleteWhere {
                (TracksTable.library eq libraryId) and (TracksTable.scanIndex less scanIndex)
            }
        }

    fun removeOrphans(libraryId: UUID): Unit =
        transaction {
            val now = Instant.now()
            // Books first: deleting them cascades the link rows away, which is what leaves the authors
            // and series below without books.
            reap(
                table = BooksTable,
                library = BooksTable.library,
                deferUntil = BooksTable.deferDeletionUntil,
                libraryId = libraryId,
                orphaned = BooksTable.id notInSubQuery TracksTable.select(TracksTable.book),
                now = now,
            )
            reap(
                table = AuthorTable,
                library = AuthorTable.library,
                deferUntil = AuthorTable.deferDeletionUntil,
                libraryId = libraryId,
                orphaned = AuthorTable.id notInSubQuery visiblyLinked(AuthorBookTable, AuthorBookTable.authors),
                now = now,
            )
            reap(
                table = SeriesTable,
                library = SeriesTable.library,
                deferUntil = SeriesTable.deferDeletionUntil,
                libraryId = libraryId,
                orphaned = SeriesTable.id notInSubQuery visiblyLinked(SeriesBookTable, SeriesBookTable.series),
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
        orphaned: Op<Boolean>,
        now: Instant,
    ) {
        val inLibrary = library eq libraryId
        table.deleteWhere { inLibrary and orphaned and (deferUntil lessEq now) }
        stampDeferral(table, deferUntil, inLibrary, orphaned, now)
    }

    fun removeOrphanedImages(): Unit =
        transaction {
            ImageTable.deleteWhere {
                ImageTable.id notInSubQuery
                    BookFileMetadataTable
                        .referenced(BookFileMetadataTable.coverID)
                        .union(BookAgentMetadataTable.referenced(BookAgentMetadataTable.coverID))
                        .union(BookUserMetadataTable.referenced(BookUserMetadataTable.coverID))
                        .union(SeriesFileMetadataTable.referenced(SeriesFileMetadataTable.coverID))
                        .union(SeriesAgentMetadataTable.referenced(SeriesAgentMetadataTable.coverID))
                        .union(SeriesUserMetadataTable.referenced(SeriesUserMetadataTable.coverID))
                        .union(AuthorFileMetadataTable.referenced(AuthorFileMetadataTable.imageID))
                        .union(AuthorAgentMetadataTable.referenced(AuthorAgentMetadataTable.imageID))
                        .union(AuthorUserMetadataTable.referenced(AuthorUserMetadataTable.imageID))
            }
        }

    private fun Table.referenced(column: Column<EntityID<UUID>?>) = select(column).where { column.isNotNull() }
}
