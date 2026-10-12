package io.thoth.openapi.ktor

class OpenApiRouteCollector {
    private val routes = mutableMapOf<String, MutableList<OpenApiRoute>>()
    private val sockets = mutableListOf<OpenApiRoute>()

    // The request body of a websocket route is what a client sends and the response body what it receives
    val webSockets: List<OpenApiRoute> get() = sockets

    fun addWebSocket(route: OpenApiRoute) {
        check(sockets.none { it.fullPath == route.fullPath }) { "Websocket ${route.fullPath} already exists" }
        sockets.add(route)
    }

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
