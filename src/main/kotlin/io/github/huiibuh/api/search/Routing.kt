package io.github.huiibuh.api.search

import io.github.huiibuh.api.exceptions.APIBadRequest
import io.github.huiibuh.api.exceptions.APINotImplemented
import io.github.huiibuh.api.exceptions.withNotImplementedRequestHandling
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import io.github.huiibuh.api.ApiTags
import io.github.huiibuh.models.SearchModel
import io.github.huiibuh.services.database.SearchService


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



