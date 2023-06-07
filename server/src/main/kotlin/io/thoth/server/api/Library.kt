package io.thoth.server.api

import io.ktor.server.routing.*
import io.thoth.generators.openapi.errors.ErrorResponse
import io.thoth.generators.openapi.get
import io.thoth.generators.openapi.patch
import io.thoth.generators.openapi.post
import io.thoth.generators.openapi.put
import io.thoth.models.LibraryModel
import io.thoth.models.SearchModel
import io.thoth.server.common.scheduling.Scheduler
import io.thoth.server.database.tables.Library
import io.thoth.server.plugins.authentication.thothPrincipal
import io.thoth.server.repositories.LibraryRepository
import io.thoth.server.repositories.SearchRepository
import io.thoth.server.schedules.ThothSchedules
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.inject

fun Routing.libraryRouting() {
    val scheduler by inject<Scheduler>()
    val schedules by inject<ThothSchedules>()

    val libraryRepository by inject<LibraryRepository>()

    get<Api.Libraries, List<LibraryModel>> { libraryRepository.getAll() }

    get<Api.Libraries.Id, LibraryModel> { (id) -> libraryRepository.get(id) }

    put<Api.Libraries.Id, LibraryApiModel, LibraryModel> { (id), postLibrary ->
        libraryRepository.replace(id, postLibrary)
    }

    patch<Api.Libraries.Id, PartialLibraryApiModel, LibraryModel> { (id), patchLibrary ->
        libraryRepository.modify(id, patchLibrary)
    }

    post<Api.Libraries.Id.Rescan, Unit, Unit> { route, _ -> libraryRepository.rescan(route.libraryId) }

    post<Api.Libraries, LibraryApiModel, LibraryModel> { _, postLibrary -> libraryRepository.create(postLibrary) }

    post<Api.Libraries.Rescan, Unit, Unit> { _, _ ->
        val libsToScan = thothPrincipal().accessToLibs
        if (libsToScan == null) scheduler.schedule(schedules.fullScan)
        else scheduler.dispatch(schedules.scanLibraries.build(libsToScan))
    }

    get<Api.Libraries.Search, SearchModel> {
        val libsToSearch = thothPrincipal().accessToLibs ?: transaction { Library.all().map { it.id.value } }

        if (it.q != null) {
            return@get SearchRepository.everywhere(it.q, libsToSearch)
        }

        throw ErrorResponse.notImplemented(
            "This is still under construction. Currently only the parameter 'q' is supported",
        )
    }
}
