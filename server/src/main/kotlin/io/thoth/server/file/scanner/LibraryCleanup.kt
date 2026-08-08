package io.thoth.server.file.scanner

import io.thoth.server.database.tables.AuthorBookTable
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.ImageTable
import io.thoth.server.database.tables.LibrariesTable
import io.thoth.server.database.tables.SeriesBookTable
import io.thoth.server.database.tables.SeriesTable
import io.thoth.server.database.tables.TracksTable
import org.jetbrains.exposed.v1.core.and
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
                    (AuthorTable.id notInSubQuery AuthorBookTable.select(AuthorBookTable.authors))
            }
            SeriesTable.deleteWhere {
                (SeriesTable.library eq libraryId) and
                    (SeriesTable.id notInSubQuery SeriesBookTable.select(SeriesBookTable.series))
            }

            ImageTable.deleteWhere {
                ImageTable.id notInSubQuery (
                    BooksTable
                        .select(BooksTable.coverID)
                        .where { BooksTable.coverID.isNotNull() }
                        .union(
                            SeriesTable.select(SeriesTable.coverID).where { SeriesTable.coverID.isNotNull() },
                        ).union(
                            AuthorTable.select(AuthorTable.imageID).where { AuthorTable.imageID.isNotNull() },
                        )
                )
            }
        }
}
