package io.github.huiibuh.api

import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.route
import io.ktor.application.*
import io.ktor.routing.*

fun Application.withBasePath(
    basePath: String,
    routeCallback: Route.() -> Unit,
    openApiCallback: NormalOpenAPIRoute.() -> Unit
) {
    routing {
        route(basePath) {
            this.apply(routeCallback)
        }
    }
    apiRouting {
        route(basePath) {
            this.apply(openApiCallback)
        }
    }
}
