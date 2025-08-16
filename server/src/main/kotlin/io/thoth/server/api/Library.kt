package io.thoth.server.api

import io.ktor.server.routing.Routing
import io.thoth.models.LibraryModel
import io.thoth.models.SearchModel
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.openapi.ktor.get
import io.thoth.openapi.ktor.patch
import io.thoth.openapi.ktor.post
import io.thoth.openapi.ktor.put
import io.thoth.server.common.scheduling.Scheduler
import io.thoth.server.plugins.auth.thothPrincipal
import io.thoth.server.repositories.LibraryRepository
import io.thoth.server.repositories.SearchRepository
import io.thoth.server.schedules.ThothSchedules
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

    get<Api.Libraries.Search, SearchModel> {
        val libsToSearch = thothPrincipal().permissions.libraries.map { lib -> lib.id }

        if (it.q != null) {
            return@get SearchRepository.everywhere(it.q, libsToSearch)
        }

        throw ErrorResponse.notImplemented(
            "This is still under construction. Currently only the parameter 'q' is supported",
        )
    }
}
