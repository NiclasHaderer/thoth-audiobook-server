package io.thoth.server.repositories

import io.thoth.generators.openapi.errors.ErrorResponse
import io.thoth.models.LibraryModel
import io.thoth.server.api.LibraryApiModel
import io.thoth.server.api.PartialLibraryApiModel
import io.thoth.server.common.scheduling.Scheduler
import io.thoth.server.database.access.toModel
import io.thoth.server.database.tables.Library
import io.thoth.server.database.tables.TLibraries
import io.thoth.server.file.scanner.FileTreeWatcher
import io.thoth.server.schedules.ThothSchedules
import java.nio.file.Path
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging.logger
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface LibraryRepository {
    fun raw(id: UUID): Library
    fun rescan(id: UUID)
    fun get(id: UUID): LibraryModel
    fun getAll(): List<LibraryModel>
    fun modify(id: UUID, partial: PartialLibraryApiModel): LibraryModel
    fun create(complete: LibraryApiModel): LibraryModel
    fun replace(id: UUID, complete: LibraryApiModel): LibraryModel
    fun overlappingFolders(id: UUID?, folders: List<String>): Pair<Boolean, List<Path>>
    fun allFolders(): List<Path>

    fun getMatching(path: Path): Library?
}

class LibraryRepositoryImpl() : LibraryRepository, KoinComponent {
    private val fileWatcher by inject<FileTreeWatcher>()
    private val scheduler by inject<Scheduler>()
    private val schedules by inject<ThothSchedules>()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val log = logger {}

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
                    coroutineScope.launch { fileWatcher.watch(allFolders()) }
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
                coroutineScope.launch { fileWatcher.watch(allFolders()) }
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
                coroutineScope.launch { fileWatcher.watch(allFolders()) }
            }
            .toModel()

    override fun overlappingFolders(id: UUID?, folders: List<String>): Pair<Boolean, List<Path>> = transaction {
        val newFolders = folders.map { Path.of(it) }
        val allFolders = Library.find { TLibraries.id neq id }.flatMap { it.folders }.map { Path.of(it) }
        val overlaps = newFolders.filter { newFolder -> allFolders.any { newFolder.startsWith(it) } }

        Pair(overlaps.isNotEmpty(), overlaps)
    }

    override fun allFolders(): List<Path> = transaction { Library.all().flatMap { it.folders }.map { Path.of(it) } }

    override fun getMatching(path: Path): Library? = transaction {
        val potentialLibraries =
            Library.all().filter { lib -> lib.folders.map { Path.of(it) }.any { path.startsWith(it) } }
        if (potentialLibraries.isEmpty()) return@transaction null
        if (potentialLibraries.size == 1) return@transaction potentialLibraries.first()
        log.error { "Multiple libraries match path $path" }
        return@transaction null
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
