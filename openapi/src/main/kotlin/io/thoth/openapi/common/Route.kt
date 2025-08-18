package io.thoth.openapi.common

import io.ktor.server.auth.AuthenticationRouteSelector
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingNode

internal fun Route.fullPath(): String {
    if (this is RoutingNode) {
        return this.fullPath()
    }
    throw IllegalStateException("Route is not a RoutingNode, cannot determine full path")
}

internal fun RoutingNode.fullPath(): String {
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
