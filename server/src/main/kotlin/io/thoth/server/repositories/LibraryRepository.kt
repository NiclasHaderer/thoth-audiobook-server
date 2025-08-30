package io.thoth.server.repositories

import io.thoth.models.Library
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.api.PartialUpdateLibrary
import io.thoth.server.api.UpdateLibrary
import io.thoth.server.common.scheduling.Scheduler
import io.thoth.server.database.tables.LibrariesTable
import io.thoth.server.database.tables.LibraryEntity
import io.thoth.server.schedules.ThothSchedules
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.nio.file.Path
import java.util.UUID

interface LibraryRepository {
    fun raw(id: UUID): LibraryEntity

    fun rescan(id: UUID)

    fun get(id: UUID): Library

    fun getAll(): List<Library>

    fun modify(
        id: UUID,
        partial: PartialUpdateLibrary,
    ): Library

    fun create(complete: UpdateLibrary): Library

    fun replace(
        id: UUID,
        complete: UpdateLibrary,
    ): Library
}

class LibraryRepositoryImpl :
    LibraryRepository,
    KoinComponent {
    private val scheduler by inject<Scheduler>()
    private val schedules by inject<ThothSchedules>()

    override fun raw(id: UUID): LibraryEntity =
        transaction {
            LibraryEntity.find { LibrariesTable.id eq id }.firstOrNull() ?: throw ErrorResponse.notFound("Library", id)
        }

    override fun rescan(id: UUID) {
        val library = raw(id)
        runBlocking { scheduler.dispatch(schedules.scanLibrary.build(library)) }
    }

    override fun get(id: UUID): Library = transaction { raw(id).toModel() }

    override fun getAll(): List<Library> = transaction { LibraryEntity.all().map { it.toModel() } }

    override fun modify(
        id: UUID,
        partial: PartialUpdateLibrary,
    ): Library =
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
                language = partial.language ?: language
            }
        }.also {
            if (partial.folders != null || partial.metadataScanners != null || partial.fileScanners != null) {
                runBlocking { scheduler.dispatch(schedules.scanLibrary.build(it)) }
            }
        }.toModel()

    override fun create(complete: UpdateLibrary): Library =
        transaction {
            raiseForOverlaps(null, complete.folders)
            LibraryEntity.new {
                name = complete.name
                icon = complete.icon
                folders = complete.folders
                preferEmbeddedMetadata = complete.preferEmbeddedMetadata
                metadataScanners = complete.metadataScanners
                fileScanners = complete.fileScanners
                language = complete.language
            }
        }.also {
            runBlocking { scheduler.dispatch(schedules.scanLibrary.build(it)) }
        }.toModel()

    override fun replace(
        id: UUID,
        complete: UpdateLibrary,
    ): Library =
        transaction {
            raiseForOverlaps(id, complete.folders)

            val library = LibraryEntity.findById(id) ?: LibraryEntity.new { name = complete.name }
            library.apply {
                name = complete.name
                icon = complete.icon
                folders = complete.folders
                preferEmbeddedMetadata = complete.preferEmbeddedMetadata
                metadataScanners = complete.metadataScanners
                fileScanners = complete.fileScanners
            }
        }.also {
            runBlocking { scheduler.dispatch(schedules.scanLibrary.build(it)) }
        }.toModel()

    fun overlappingFolders(
        id: UUID?,
        folders: List<String>,
    ): Pair<Boolean, List<Path>> =
        transaction {
            val newFolders = folders.map { Path.of(it) }
            val allFolders = LibraryEntity.find { LibrariesTable.id neq id }.flatMap { it.folders }.map { Path.of(it) }
            val overlaps = newFolders.filter { newFolder -> allFolders.any { newFolder.startsWith(it) } }

            Pair(overlaps.isNotEmpty(), overlaps)
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
