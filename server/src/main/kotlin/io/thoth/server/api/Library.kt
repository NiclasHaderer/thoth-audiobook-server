package io.thoth.server.api

import io.ktor.http.*
import io.ktor.server.routing.*
import io.thoth.common.scheduling.Scheduler
import io.thoth.database.access.allFolders
import io.thoth.database.access.foldersOverlap
import io.thoth.database.access.toModel
import io.thoth.database.tables.Library
import io.thoth.models.LibraryModel
import io.thoth.models.SearchModel
import io.thoth.openapi.routing.RouteHandler
import io.thoth.openapi.routing.get
import io.thoth.openapi.routing.patch
import io.thoth.openapi.routing.post
import io.thoth.openapi.serverError
import io.thoth.server.file.scanner.FileTreeWatcher
import io.thoth.server.schedules.ThothSchedules
import io.thoth.server.services.SearchService
import java.util.*
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.inject

private fun RouteHandler.validateFolders(id: UUID, folders: List<String>) {
    val (overlaps, overlapping) = Library.foldersOverlap(id, folders)
    if (!overlaps)
        serverError(
            HttpStatusCode.Conflict,
            "Folders overlap with existing libraries",
            mapOf(
                "overlaps" to overlapping,
                "library" to id,
            ),
        )
}

fun Routing.libraryRouting() {
    val fileWatcher by inject<FileTreeWatcher>()
    val scheduler by inject<Scheduler>()
    val schedules by inject<ThothSchedules>()

    get<Api.Libraries, List<LibraryModel>> { transaction { Library.all().map { it.toModel() } } }

    get<Api.Libraries.Id, LibraryModel> { (id) ->
        transaction { Library.findById(id)?.toModel() ?: serverError(HttpStatusCode.NotFound, "Library was not found") }
    }

    post<Api.Libraries.Id, PostLibrary, LibraryModel> { (id), postLibrary ->
        transaction {
                validateFolders(id, postLibrary.folders)

                val library = Library.findById(id) ?: Library.new { name = postLibrary.name }
                library.apply {
                    name = postLibrary.name
                    icon = postLibrary.icon
                    folders = postLibrary.folders
                    preferEmbeddedMetadata = postLibrary.preferEmbeddedMetadata
                    metadataScanners = postLibrary.metadataScanners
                    fileScanners = postLibrary.fileScanners
                }
            }
            .also {
                scheduler.dispatch(schedules.scanLibrary.build(it))
                launch { fileWatcher.watch(Library.allFolders()) }
            }
            .toModel()
    }

    patch<Api.Libraries.Id, PatchLibrary, LibraryModel> { (id), patchLibrary ->
        transaction {
                if (patchLibrary.folders != null) {
                    validateFolders(id, patchLibrary.folders)
                }

                val library = Library.findById(id) ?: serverError(HttpStatusCode.NotFound, "Library was not found")
                library.apply {
                    name = patchLibrary.name ?: name
                    icon = patchLibrary.icon ?: icon
                    folders = patchLibrary.folders ?: folders
                    preferEmbeddedMetadata = patchLibrary.preferEmbeddedMetadata ?: preferEmbeddedMetadata
                    metadataScanners = patchLibrary.metadataScanners ?: metadataScanners
                    fileScanners = patchLibrary.fileScanners ?: fileScanners
                }
            }
            .also {
                if (
                    patchLibrary.folders != null ||
                        patchLibrary.metadataScanners != null ||
                        patchLibrary.fileScanners != null
                ) {
                    scheduler.dispatch(schedules.scanLibrary.build(it))
                    launch { fileWatcher.watch(Library.allFolders()) }
                }
            }
            .toModel()
    }

    post<Api.Libraries.Id.Rescan, Unit, Unit> { route, _ ->
        val library =
            transaction { Library.findById(route.libraryId) }
                ?: serverError(
                    HttpStatusCode.BadRequest,
                    "Library with id ${route.libraryId} not found",
                )
        scheduler.dispatch(schedules.scanLibrary.build(library))
    }

    post<Api.Libraries.Rescan, Unit, Unit> { _, _ -> scheduler.launchScheduledJob(schedules.fullScan) }

    get<Api.Libraries.Search, SearchModel> {
        if (it.q != null) {
            return@get SearchService.everywhere(it.q)
        }

        serverError(
            HttpStatusCode.NotImplemented,
            "This is still under construction. Currently only the parameter 'q' is supported",
        )
    }
}
