package io.thoth.openapi.openapi

import kotlin.collections.set

object OpenApiRouteCollector {
    private val routes = mutableMapOf<String, MutableList<OpenApiRoute>>()

    fun addRoute(route: OpenApiRoute) {
        if (route.fullPath !in routes) {
            routes[route.fullPath] = mutableListOf()
        }

        val routeExists = routes[route.fullPath]!!.any { it.method == route.method }
        if (routeExists) {
            throw IllegalStateException("Route ${route.method}:${route.fullPath} already exists")
        }
        routes[route.fullPath]!!.add(route)
    }

    fun forEach(action: (OpenApiRoute) -> Unit) {
        routes.values.forEach { it.forEach(action) }
    }

    fun values() = routes.values.flatten()
}
