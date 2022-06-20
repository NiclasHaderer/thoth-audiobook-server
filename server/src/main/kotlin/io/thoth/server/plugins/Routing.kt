package io.thoth.server.plugins

import io.ktor.server.application.*
import io.ktor.server.locations.*

@OptIn(KtorExperimentalLocationsAPI::class)
fun Application.configureRouting() {
    install(Locations)
}
