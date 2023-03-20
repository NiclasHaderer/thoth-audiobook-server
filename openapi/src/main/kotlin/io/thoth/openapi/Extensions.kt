package io.thoth.openapi

import io.ktor.server.auth.*
import io.ktor.server.routing.*

internal fun Route.fullPath(): String {
    var fullPath = "/${selector}"
    var routeParent = parent
    while (routeParent != null) {
        if (routeParent.selector !is AuthenticationRouteSelector) {
            fullPath = "/${routeParent.selector}$fullPath"
        }
        routeParent = routeParent.parent
    }

    return fullPath.replace("/+".toRegex(), "/")
}
