package io.thoth.server.api

import io.ktor.server.routing.Routing
import io.thoth.openapi.ktor.post

fun Routing.pingRouting() {
    post<Api.Ping, Unit, Unit> { _, _ -> }
}
