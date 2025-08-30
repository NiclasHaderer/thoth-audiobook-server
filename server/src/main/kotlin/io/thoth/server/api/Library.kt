package io.thoth.server.api

import io.ktor.server.routing.Routing
import io.thoth.models.Library
import io.thoth.models.LibrarySearchResult
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.openapi.ktor.get
import io.thoth.openapi.ktor.patch
import io.thoth.openapi.ktor.post
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

    get<Api.Libraries, List<Library>> { libraryRepository.getAll() }

    get<Api.Libraries.Id, Library> { (id) -> libraryRepository.get(id) }

    patch<Api.Libraries.Id, PartialUpdateLibrary, Library> { (id), patchLibrary ->
        libraryRepository.modify(id, patchLibrary)
    }

    post<Api.Libraries.Id.Rescan, Unit, Unit> { route, _ -> libraryRepository.rescan(route.libraryId) }

    post<Api.Libraries, UpdateLibrary, Library> { _, postLibrary ->
        val principal = thothPrincipal()
        if (!principal.permissions.isAdmin) {
            throw ErrorResponse.forbidden(
                "Create",
                "library",
                "Only admins can create libraries",
            )
        }
        libraryRepository.create(postLibrary)
    }

    get<Api.Libraries.Search, LibrarySearchResult> {
        val libsToSearch = thothPrincipal().permissions.libraries.map { lib -> lib.id }

        if (it.q != null) {
            return@get SearchRepository.everywhere(it.q, libsToSearch)
        }

        throw ErrorResponse.notImplemented(
            "This is still under construction. Currently only the parameter 'q' is supported",
        )
    }
}
