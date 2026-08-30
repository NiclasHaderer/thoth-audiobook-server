package io.thoth.openapi.ktor

import io.ktor.server.routing.RoutingContext
import io.ktor.util.AttributeKey
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

interface BeforeBodyParsing {
    suspend fun RoutingContext.beforeBodyParsing()
}

/**
 * The resource the request actually resolved to. An enclosing route's hook only ever sees itself, so
 * this is how a parent check can tell which of its sub-routes it is running for.
 */
val RouteParamsKey = AttributeKey<Any>("openapi.routeParams")

// Walks the `parent` chain so an enclosing route's hook (e.g. per-library checks on {libraryId}) also runs for sub-routes.
suspend fun RoutingContext.runBeforeBodyParsing(params: Any) {
    call.attributes.put(RouteParamsKey, params)
    val chain = mutableListOf<Any>()
    var current: Any? = params
    while (current != null && chain.none { it === current }) {
        chain.add(current)
        current =
            current::class
                .memberProperties
                .firstOrNull { it.name == "parent" }
                ?.also { it.isAccessible = true }
                ?.getter
                ?.call(current)
    }
    chain.asReversed().forEach { node ->
        if (node is BeforeBodyParsing) node.run { beforeBodyParsing() }
    }
}

interface AfterBodyParsing {
    suspend fun RoutingContext.afterBodyParsing()
}

interface ValidateObject {
    suspend fun RoutingContext.validateBody()
}

interface AfterResponse {
    suspend fun RoutingContext.afterResponse()
}
