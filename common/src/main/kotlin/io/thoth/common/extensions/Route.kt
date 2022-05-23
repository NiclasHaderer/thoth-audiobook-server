package io.thoth.common.extensions

import io.ktor.routing.*

val Route.fullPath: String
    get() {
        var fullPath = selector.toString()
        var routeParent = parent
        while (routeParent != null) {
            fullPath = "${routeParent.selector}/$fullPath"
            routeParent = routeParent.parent
        }
        return fullPath
    }
