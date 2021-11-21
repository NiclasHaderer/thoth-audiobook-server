package io.github.huiibuh.api.audiobooks.search

import api.exceptions.APIBadRequest
import api.exceptions.APINotImplemented
import api.exceptions.withNotImplementedRequestHandling
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.github.huiibuh.api.images.imageRouting
import io.github.huiibuh.models.SearchModel
import io.github.huiibuh.services.database.SearchService


fun NormalOpenAPIRoute.searchRouting(route: String = "search") {
    route(route) {
        imageRouting()
    }
}


internal fun NormalOpenAPIRoute.searchRouting() {

    withNotImplementedRequestHandling {
        get<SearchQuery, SearchModel> {

            if (it.q != null) {
                return@get respond(
                    SearchService.everywhere(it.q)
                )
            }
            if (it.series == null && it.author == null && it.book == null) {
                throw APIBadRequest("At least one query parameter has to have a value")
            }

            throw APINotImplemented("This is still under construction. Currently only the parameter 'q' is supported")

        }
    }
}



