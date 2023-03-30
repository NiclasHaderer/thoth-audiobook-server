package io.thoth.server.services

import io.thoth.common.scheduling.Scheduler
import io.thoth.database.access.allFolders
import io.thoth.database.access.toModel
import io.thoth.database.tables.Library
import io.thoth.database.tables.TLibraries
import io.thoth.models.LibraryModel
import io.thoth.openapi.ErrorResponse
import io.thoth.server.api.LibraryApiModel
import io.thoth.server.api.PartialLibraryApiModel
import io.thoth.server.file.scanner.FileTreeWatcher
import io.thoth.server.schedules.ThothSchedules
import java.nio.file.Path
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.transactions.transaction

interface LibraryRepository {
    fun raw(id: UUID): Library
    fun rescan(id: UUID)
    fun get(id: UUID): LibraryModel
    fun getAll(): List<LibraryModel>
    fun modify(id: UUID, partial: PartialLibraryApiModel): LibraryModel
    fun create(complete: LibraryApiModel): LibraryModel
    fun replace(id: UUID, complete: LibraryApiModel): LibraryModel
    fun overlappingFolders(id: UUID?, folders: List<String>): Pair<Boolean, List<Path>>
}

class LibraryRepositoryImpl(
    private val fileWatcher: FileTreeWatcher,
    private val scheduler: Scheduler,
    private val schedules: ThothSchedules
) : LibraryRepository {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun raw(id: UUID): Library = transaction {
        Library.find { TLibraries.id eq id }.firstOrNull() ?: throw ErrorResponse.notFound("Library", id)
    }

    override fun rescan(id: UUID) {
        val library = raw(id)
        runBlocking { scheduler.dispatch(schedules.scanLibrary.build(library)) }
    }

    override fun get(id: UUID): LibraryModel = transaction { raw(id).toModel() }

    override fun getAll(): List<LibraryModel> = transaction { Library.all().map { it.toModel() } }

    override fun modify(id: UUID, partial: PartialLibraryApiModel): LibraryModel =
        transaction {
                if (partial.folders != null) {
                    raiseForOverlaps(id, partial.folders)
                }

                val library = raw(id)
                library.apply {
                    name = partial.name ?: name
                    icon = partial.icon ?: icon
                    folders = partial.folders ?: folders
                    preferEmbeddedMetadata = partial.preferEmbeddedMetadata ?: preferEmbeddedMetadata
                    metadataScanners = partial.metadataScanners ?: metadataScanners
                    fileScanners = partial.fileScanners ?: fileScanners
                }
            }
            .also {
                if (partial.folders != null || partial.metadataScanners != null || partial.fileScanners != null) {
                    runBlocking { scheduler.dispatch(schedules.scanLibrary.build(it)) }
                    coroutineScope.launch { fileWatcher.watch(Library.allFolders()) }
                }
            }
            .toModel()

    override fun create(complete: LibraryApiModel): LibraryModel =
        transaction {
                raiseForOverlaps(null, complete.folders)
                Library.new {
                    name = complete.name
                    icon = complete.icon
                    folders = complete.folders
                    preferEmbeddedMetadata = complete.preferEmbeddedMetadata
                    metadataScanners = complete.metadataScanners
                    fileScanners = complete.fileScanners
                }
            }
            .also {
                runBlocking { scheduler.dispatch(schedules.scanLibrary.build(it)) }
                coroutineScope.launch { fileWatcher.watch(Library.allFolders()) }
            }
            .toModel()

    override fun replace(id: UUID, complete: LibraryApiModel): LibraryModel =
        transaction {
                raiseForOverlaps(id, complete.folders)

                val library = Library.findById(id) ?: Library.new { name = complete.name }
                library.apply {
                    name = complete.name
                    icon = complete.icon
                    folders = complete.folders
                    preferEmbeddedMetadata = complete.preferEmbeddedMetadata
                    metadataScanners = complete.metadataScanners
                    fileScanners = complete.fileScanners
                }
            }
            .also {
                runBlocking { scheduler.dispatch(schedules.scanLibrary.build(it)) }
                coroutineScope.launch { fileWatcher.watch(Library.allFolders()) }
            }
            .toModel()

    override fun overlappingFolders(id: UUID?, folders: List<String>): Pair<Boolean, List<Path>> = transaction {
        val newFolders = folders.map { Path.of(it) }
        val allFolders = Library.find { TLibraries.id neq id }.flatMap { it.folders }.map { Path.of(it) }
        val overlaps = newFolders.filter { newFolder -> allFolders.any { it.contains(newFolder) } }

        Pair(overlaps.isEmpty(), overlaps)
    }

    private fun raiseForOverlaps(libraryId: UUID?, folders: List<String>) {
        val (overlaps, overlapping) = overlappingFolders(libraryId, folders)

        if (overlaps) {
            throw ErrorResponse.userError(
                "Folders overlap with existing libraries",
                mapOf(
                    "overlaps" to overlapping,
                    "library" to libraryId,
                ),
            )
        }
    }
}
