package io.thoth.server.api

import io.ktor.http.*
import io.ktor.server.routing.*
import io.thoth.models.SearchModel
import io.thoth.openapi.routing.get
import io.thoth.openapi.serverError
import io.thoth.server.services.SearchService

fun Routing.searchRouting() {
    get<Api.Search, SearchModel> {
        if (it.q != null) {
            return@get SearchService.everywhere(it.q)
        }

        serverError(
            HttpStatusCode.NotImplemented,
            "This is still under construction. Currently only the parameter 'q' is supported",
        )
    }
}
