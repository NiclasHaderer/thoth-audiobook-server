package io.thoth.server.api.v1

import io.ktor.server.routing.*
import io.thoth.openapi.routing.get
import io.thoth.server.api.Api

fun Routing.pingRouting() {
    get<Api.Ping, Unit> {}
}
