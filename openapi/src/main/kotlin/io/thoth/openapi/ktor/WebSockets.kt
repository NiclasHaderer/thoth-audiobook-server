package io.thoth.openapi.ktor

import io.ktor.server.application.plugin
import io.ktor.server.auth.authenticate
import io.ktor.server.resources.Resources
import io.ktor.server.resources.resource
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.webSocket
import io.thoth.openapi.common.findAnnotationUp
import kotlinx.serialization.serializer
import kotlin.reflect.full.findAnnotation

inline fun <reified PARAMS : Any> Route.guarded(crossinline build: Route.() -> Unit) {
    val ignoreSecured = PARAMS::class.findAnnotation<NotSecured>() != null
    val secured = PARAMS::class.findAnnotationUp<Secured>()
    if (secured != null && !ignoreSecured) {
        authenticate(secured.name) { build() }
    } else {
        build()
    }
}

// OpenAPI cannot describe a websocket, so the route is guarded and typed like the others but left out of the spec
inline fun <reified PARAMS : Any> Route.webSocket(
    noinline handler: suspend DefaultWebSocketServerSession.(params: PARAMS) -> Unit,
) {
    val format = application.plugin(Resources).resourcesFormat
    val serializer = serializer<PARAMS>()
    guarded<PARAMS> {
        resource<PARAMS> {
            webSocket { handler(format.decodeFromParameters(serializer, call.parameters)) }
        }
    }
}
