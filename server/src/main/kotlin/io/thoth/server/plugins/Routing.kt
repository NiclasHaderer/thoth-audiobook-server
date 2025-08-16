package io.thoth.server.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.resources.Resources

fun Application.configureRouting() {
    install(Resources)
}
