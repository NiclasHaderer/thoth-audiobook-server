package io.thoth.server.api

import io.ktor.server.routing.*
import io.thoth.generators.openapi.get

fun Routing.pingRouting() {
    get<Api.Ping, Unit> {}
}
