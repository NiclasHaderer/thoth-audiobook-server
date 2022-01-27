package io.github.huiibuh.plugins

import io.ktor.application.*
import io.ktor.locations.*

@OptIn(KtorExperimentalLocationsAPI::class)
fun Application.configureRouting() {
    install(Locations)
}
