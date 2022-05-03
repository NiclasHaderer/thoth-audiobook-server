package io.thoth.server.api.search

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import io.thoth.common.exceptions.APIBadRequest
import io.thoth.common.exceptions.APINotImplemented
import io.thoth.common.exceptions.withNotImplementedRequestHandling
import io.thoth.models.SearchModel
import io.thoth.server.api.ApiTags
import io.thoth.server.services.SearchService


fun NormalOpenAPIRoute.registerSearchRouting(route: String = "search") {
    route(route) {
        tag(ApiTags.Search) {
            routing()
        }
    }
}


internal fun NormalOpenAPIRoute.routing() {

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



