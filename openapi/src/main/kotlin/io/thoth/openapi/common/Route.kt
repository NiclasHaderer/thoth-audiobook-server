package io.thoth.openapi.common

import io.ktor.server.auth.AuthenticationRouteSelector
import io.ktor.server.routing.Route

internal fun Route.fullPath(): String {
    var fullPath = "/$selector"
    var routeParent = parent
    while (routeParent != null) {
        if (routeParent.selector !is AuthenticationRouteSelector) {
            fullPath = "/${routeParent.selector}$fullPath"
        }
        routeParent = routeParent.parent
    }

    return fullPath.replace("/+".toRegex(), "/")
}
