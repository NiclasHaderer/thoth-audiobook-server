package io.thoth.common.extensions

import io.ktor.server.auth.*
import io.ktor.server.routing.*

val Route.fullPath: String
  get() {
    var fullPath = selector.toString()
    var routeParent = parent
    while (routeParent != null) {
      if (routeParent.selector !is AuthenticationRouteSelector) {
        fullPath = "${routeParent.selector}/$fullPath"
      }
      routeParent = routeParent.parent
    }
    return fullPath
  }
