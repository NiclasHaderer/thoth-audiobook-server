package io.thoth.server.repositories

import io.thoth.models.Library
import io.thoth.openapi.common.ifSet
import io.thoth.openapi.common.isSet
import io.thoth.openapi.common.orElse
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.api.PartialUpdateLibrary
import io.thoth.server.api.UpdateLibrary
import io.thoth.server.common.extensions.canonical
import io.thoth.server.common.scheduling.Scheduler
import io.thoth.server.database.tables.BookTable
import io.thoth.server.database.tables.LibraryRow
import io.thoth.server.database.tables.LibraryTable
import io.thoth.server.database.tables.insert
import io.thoth.server.database.tables.toLibraryRow
import io.thoth.server.database.tables.update
import io.thoth.server.file.scanner.LibraryCleanup
import io.thoth.server.file.scanner.LibraryRoots
import io.thoth.server.file.scanner.LibraryWatcher
import io.thoth.server.file.scanner.ScanRequest
import io.thoth.server.schedules.ThothSchedules
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

// Serializes library create/modify so the folder-overlap check-then-write can't race (two concurrent
// mutations both passing the overlap check). In-process lock: fine for a single app instance.
private val libraryMutationLock = ReentrantLock()

interface LibraryRepository {
    fun raw(id: UUID): Library

    fun rescan(id: UUID)

    fun get(id: UUID): Library

    fun getAll(): List<Library>

    fun modify(
        id: UUID,
        partial: PartialUpdateLibrary,
    ): Library

    fun create(complete: UpdateLibrary): Library

    fun delete(id: UUID)
}

class LibraryRepositoryImpl :
    LibraryRepository,
    KoinComponent {
    private val scheduler by inject<Scheduler>()
    private val schedules by inject<ThothSchedules>()
    private val watcher by inject<LibraryWatcher>()
    private val roots by inject<LibraryRoots>()
    private val cleanup by inject<LibraryCleanup>()

    override fun raw(id: UUID): Library = transaction { rawRow(id).toModel(bookCount(id)) }

    private fun bookCount(id: UUID): Long =
        BookTable
            .selectAll()
            .where { (BookTable.library eq id) and BookTable.deferDeletionUntil.isNull() }
            .count()

    private fun rawRow(id: UUID): LibraryRow =
        transaction {
            LibraryTable
                .selectAll()
                .where { LibraryTable.id eq id }
                .firstOrNull()
                ?.toLibraryRow()
                ?: throw ErrorResponse.notFound("Library", id)
        }

    override fun rescan(id: UUID) {
        val library = raw(id)
        scheduler.dispatch(schedules.scanLibrary.build(ScanRequest(library.id)))
    }

    override fun get(id: UUID): Library = raw(id)

    override fun getAll(): List<Library> =
        transaction { LibraryTable.selectAll().map { it.toLibraryRow() }.map { it.toModel(bookCount(it.id)) } }

    override fun modify(
        id: UUID,
        partial: PartialUpdateLibrary,
    ): Library =
        libraryMutationLock.withLock {
            // Which source wins is applied when a file is read, so a library that flipped it reads them all again
            val reanalyze =
                partial.fileScanners.isSet || partial.combineFileScannerFields.isSet ||
                    partial.preferEmbeddedMetadata.isSet
            val needsScan = partial.folders.isSet || partial.metadataAgents.isSet || reanalyze
            val model =
                transaction {
                    partial.folders.ifSet { raiseForOverlaps(id, it) }

                    val library = rawRow(id)
                    val updated =
                        library.copy(
                            name = partial.name.orElse(library.name),
                            icon = partial.icon.orElse(library.icon),
                            folders = partial.folders.orElse(library.folders),
                            preferEmbeddedMetadata = partial.preferEmbeddedMetadata.orElse(
                                library.preferEmbeddedMetadata,
                            ),
                            metadataAgents = partial.metadataAgents.orElse(library.metadataAgents),
                            combineMetadataAgentFields =
                                partial.combineMetadataAgentFields.orElse(library.combineMetadataAgentFields),
                            fileScanners = partial.fileScanners.orElse(library.fileScanners),
                            combineFileScannerFields =
                                partial.combineFileScannerFields.orElse(library.combineFileScannerFields),
                            language = partial.language.orElse(library.language),
                            region = partial.region.orElse(library.region),
                        )
                    LibraryTable.update(updated)
                    updated.toModel(bookCount(id))
                }

            if (needsScan) {
                watcher.restart()
                scheduler.dispatch(schedules.scanLibrary.build(ScanRequest(model.id, reanalyze = reanalyze)))
            }
            model
        }

    override fun create(complete: UpdateLibrary): Library =
        libraryMutationLock.withLock {
            val model =
                transaction {
                    raiseForOverlaps(null, complete.folders)
                    val row =
                        LibraryRow(
                            id = UUID.randomUUID(),
                            name = complete.name,
                            icon = complete.icon,
                            scanIndex = 0uL,
                            folders = complete.folders,
                            preferEmbeddedMetadata = complete.preferEmbeddedMetadata,
                            metadataAgents = complete.metadataAgents,
                            combineMetadataAgentFields = complete.combineMetadataAgentFields,
                            fileScanners = complete.fileScanners,
                            combineFileScannerFields = complete.combineFileScannerFields,
                            language = complete.language,
                            region = complete.region,
                        )
                    LibraryTable.insert(row)
                    row.toModel(0)
                }

            watcher.restart()
            scheduler.dispatch(schedules.scanLibrary.build(ScanRequest(model.id)))
            model
        }

    override fun delete(id: UUID) {
        libraryMutationLock.withLock {
            transaction {
                val deleted = LibraryTable.deleteWhere { LibraryTable.id eq id }
                if (deleted == 0) throw ErrorResponse.notFound("Library", id)
                cleanup.removeOrphanedImages()
            }
            watcher.restart()
        }
    }

    fun overlappingFolders(
        id: UUID?,
        folders: List<String>,
    ): Pair<Boolean, List<Path>> {
        // Both sides have to be canonicalised the same way for the comparison to mean anything, so the
        // existing side comes from the same projection the scanner compares paths against
        val newFolders = folders.map { Path.of(it).canonical() }
        val allFolders = roots.all().filter { it.id != id }.flatMap { it.folders }
        // Either direction is a conflict: a new folder nested inside an existing one, or an existing one
        // nested inside a new one.
        val overlaps =
            newFolders.filter { newFolder ->
                allFolders.any { newFolder.startsWith(it) || it.startsWith(newFolder) }
            }

        return Pair(overlaps.isNotEmpty(), overlaps)
    }

    private fun raiseForOverlaps(
        libraryId: UUID?,
        folders: List<String>,
    ) {
        val (overlaps, overlapping) = overlappingFolders(libraryId, folders)

        if (overlaps) {
            throw ErrorResponse.userError(
                "Folders overlap with existing libraries",
                mapOf("overlaps" to overlapping, "library" to libraryId),
            )
        }
    }
}
