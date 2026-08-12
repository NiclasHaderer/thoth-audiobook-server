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
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.notInSubQuery
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.union
import java.util.UUID

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
            // Books first: deleting them cascades the link rows away, which is what leaves the authors
            // and series below without books.
            BooksTable.deleteWhere {
                (BooksTable.library eq libraryId) and
                    (BooksTable.id notInSubQuery TracksTable.select(TracksTable.book))
            }
            AuthorTable.deleteWhere {
                (AuthorTable.library eq libraryId) and
                    (
                        AuthorTable.id notInSubQuery AuthorBookTable.select(AuthorBookTable.authors)
                    )
            }
            SeriesTable.deleteWhere {
                (SeriesTable.library eq libraryId) and
                    (
                        SeriesTable.id notInSubQuery SeriesBookTable.select(SeriesBookTable.series)
                    )
            }

            removeOrphanedImages()
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
