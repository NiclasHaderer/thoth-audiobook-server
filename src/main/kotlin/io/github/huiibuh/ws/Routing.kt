package io.github.huiibuh.ws

import io.github.huiibuh.db.tables.Albums
import io.github.huiibuh.db.tables.Artists
import io.github.huiibuh.db.tables.Collections
import io.ktor.routing.*


fun Route.registerUpdateRoutes(path: String = "ws") {
    route(path) {
        updateForTables(
            Artists,
            Albums,
            Collections,
        )
    }
}


