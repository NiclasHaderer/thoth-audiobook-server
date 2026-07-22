package io.thoth.openapi.ktor

import io.ktor.server.routing.RoutingContext
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

interface BeforeBodyParsing {
    suspend fun RoutingContext.beforeBodyParsing()
}

// Walks the `parent` chain so an enclosing route's hook (e.g. per-library checks on {libraryId}) also runs for sub-routes.
suspend fun RoutingContext.runBeforeBodyParsing(params: Any) {
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
