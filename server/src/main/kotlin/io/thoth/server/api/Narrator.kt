package io.thoth.server.api

import io.ktor.server.routing.Routing
import io.thoth.models.Narrator
import io.thoth.models.NarratorDetailed
import io.thoth.models.PaginatedResponse
import io.thoth.openapi.ktor.get
import io.thoth.server.repositories.NarratorRepository
import org.koin.ktor.ext.inject

fun Routing.narratorRouting() {
    val narratorRepository by inject<NarratorRepository>()

    get<Api.Libraries.Id.Narrators.All, PaginatedResponse<Narrator>> {
        PaginatedResponse(
            items = narratorRepository.getAll(it.libraryId, it.order.toSortOrder(), it.limit, it.offset),
            limit = it.limit,
            offset = it.offset,
            total = narratorRepository.total(it.libraryId),
        )
    }

    get<Api.Libraries.Id.Narrators.Name, NarratorDetailed> { narratorRepository.get(it.name, it.libraryId) }
}
