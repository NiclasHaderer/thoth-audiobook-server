package io.thoth.openapi.routing

import io.ktor.resources.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.thoth.common.extensions.parent
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

fun resolveResourcePath(resourceClassParam: KClass<*>?): String? {
    var resourcePath = ""
    var resourceClass: KClass<*>? = resourceClassParam
    while (resourceClass != null) {
        val resource = resourceClass.findAnnotation<Resource>()
        if (resource != null && resource.path.isNotBlank()) {
            resourcePath = "${resource.path}/$resourcePath"
        }
        resourceClass = resourceClass.parent
    }
    return resourcePath.ifBlank { null }
}

fun Route.fullPath(resource: KClass<*>? = null): String {
    var fullPath = "/${selector}"
    var routeParent = parent
    while (routeParent != null) {
        if (routeParent.selector !is AuthenticationRouteSelector) {
            fullPath = "/${routeParent.selector}$fullPath"
        }
        routeParent = routeParent.parent
    }

    val resourcePath = resolveResourcePath(resource)
    if (resourcePath != null) {
        fullPath += "/$resourcePath"
    }

    return fullPath.replace("/+".toRegex(), "/")
}
