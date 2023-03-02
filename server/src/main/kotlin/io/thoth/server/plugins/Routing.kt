package io.thoth.server.plugins

import io.ktor.server.application.*
import io.ktor.server.resources.*

fun Application.configureRouting() {
  install(Resources)
}
