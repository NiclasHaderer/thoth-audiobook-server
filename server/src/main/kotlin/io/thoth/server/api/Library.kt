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
import io.thoth.server.schedules.ThothSchedules
import io.thoth.server.services.LibraryRepository
import io.thoth.server.services.SearchService
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

    post<Api.Libraries.Rescan, Unit, Unit> { _, _ -> scheduler.launchScheduledJob(schedules.fullScan) }

    get<Api.Libraries.Search, SearchModel> {
        if (it.q != null) {
            return@get SearchService.everywhere(it.q)
        }

        throw ErrorResponse.notImplemented(
            "This is still under construction. Currently only the parameter 'q' is supported",
        )
    }
}
