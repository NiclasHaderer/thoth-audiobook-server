package io.thoth.server.file.scanner

import io.thoth.server.common.extensions.findOne
import io.thoth.server.common.extensions.withGuard
import io.thoth.server.database.access.hasBeenUpdated
import io.thoth.server.database.access.markAsTouched
import io.thoth.server.database.tables.AuthorBookTable
import io.thoth.server.database.tables.AuthorEntity
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.BookEntity
import io.thoth.server.database.tables.BooksTable
import io.thoth.server.database.tables.ImageTable
import io.thoth.server.database.tables.LibraryEntity
import io.thoth.server.database.tables.SeriesBookTable
import io.thoth.server.database.tables.SeriesEntity
import io.thoth.server.database.tables.SeriesTable
import io.thoth.server.database.tables.TrackEntity
import io.thoth.server.database.tables.TracksTable
import io.thoth.server.file.TrackManager
import kotlinx.coroutines.sync.Mutex
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.union
import org.koin.core.component.KoinComponent
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.getLastModifiedTime

interface LibraryScanner {
    fun scanLibrary(library: LibraryEntity)

    fun scanFolder(
        folder: Path,
        library: LibraryEntity,
    )

    fun cleanupLibrary(library: LibraryEntity)

    fun shouldUpdate(path: Path): Boolean

    fun rootOf(
        path: Path,
        library: LibraryEntity,
    ): Path?
}

class LibraryScannerImpl :
    LibraryScanner,
    KoinComponent {
    companion object {
        private val mutex = Mutex()
        private val currentLibraryScans = mutableMapOf<UUID, Boolean>()
        private val log = logger {}
    }

    override fun scanLibrary(library: LibraryEntity) {
        // Claimed before the try, so bailing out cannot run the finally and clear the running scan's flag
        val claimed =
            mutex.withGuard {
                if (currentLibraryScans[library.id.value] == true) {
                    false
                } else {
                    currentLibraryScans[library.id.value] = true
                    true
                }
            }
        if (!claimed) {
            log.info { "Skipping scan for library ${library.name}. Scan is already ongoing." }
            return
        }

        try {
            log.info { "Scanning library ${library.name}" }
            transaction { library.scanIndex += 1u }

            for (folder in library.folders.map { libraryRoot(it) }) {
                scanFolder(folder, library)
            }
            cleanupLibrary(library)
        } finally {
            mutex.withGuard { currentLibraryScans[library.id.value] = false }
        }
    }

    override fun cleanupLibrary(library: LibraryEntity): Unit =
        transaction {
            TracksTable.deleteWhere {
                (TracksTable.library eq library.id) and (TracksTable.scanIndex less library.scanIndex)
            }
            // Books first: deleting them cascades the link rows away, which is what leaves the authors
            // and series below without books.
            BooksTable.deleteWhere {
                (BooksTable.library eq library.id) and
                    (BooksTable.id notInSubQuery TracksTable.select(TracksTable.book))
            }
            AuthorTable.deleteWhere {
                (AuthorTable.library eq library.id) and
                    (AuthorTable.id notInSubQuery AuthorBookTable.select(AuthorBookTable.authors))
            }
            SeriesTable.deleteWhere {
                (SeriesTable.library eq library.id) and
                    (SeriesTable.id notInSubQuery SeriesBookTable.select(SeriesBookTable.series))
            }

            // Delete unused images
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

    override fun scanFolder(
        folder: Path,
        library: LibraryEntity,
    ) {
        // Resolved here so everything the walk hands downstream is already canonical
        val target = realPath(folder)
        if (rootOf(target, library)?.let { isIgnored(target, it) } != false) {
            log.info { "Skipping '$folder' because it is ignored or outside the library" }
            return
        }

        walkFiles(
            target,
            ignoreFolder = { TrackManager.removeFolder(it, library) },
            addOrUpdate = { path, _ ->
                if (shouldUpdate(path)) {
                    TrackManager.addPath(path, library)
                }
            },
        )
    }

    override fun rootOf(
        path: Path,
        library: LibraryEntity,
    ): Path? {
        val target = realPath(path)
        return library.folders.map { libraryRoot(it) }.firstOrNull { target.startsWith(it) }
    }

    override fun shouldUpdate(path: Path): Boolean =
        transaction {
            val dbTrack =
                TrackEntity.findOne { TracksTable.path eq path.absolutePathString() } ?: return@transaction true
            // If the track has already been imported and the access time has not changed skip
            if (dbTrack.hasBeenUpdated(path.getLastModifiedTime().toMillis())) return@transaction true
            // Mark as touched, so the tracks don't get removed
            dbTrack.markAsTouched()
            false
        }
}
