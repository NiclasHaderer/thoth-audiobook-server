package io.thoth.server.api.search

import io.ktor.http.*
import io.ktor.server.routing.*
import io.thoth.models.SearchModel
import io.thoth.openapi.routing.get
import io.thoth.openapi.serverError
import io.thoth.server.services.SearchService

fun Route.registerSearchRouting(route: String = "search") {
    route(route) { routing() }
}

private fun Route.routing() {
    get<SearchQuery, SearchModel> {
        if (it.q != null) {
            return@get SearchService.everywhere(it.q)
        }

        if (it.series == null && it.author == null && it.book == null) {
            serverError(HttpStatusCode.BadRequest, "At least one query parameter has to have a value")
        }

        serverError(
            HttpStatusCode.NotImplemented,
            "This is still under construction. Currently only the parameter 'q' is supported"
        )
    }
}
