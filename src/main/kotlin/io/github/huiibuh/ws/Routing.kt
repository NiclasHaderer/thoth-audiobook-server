package io.github.huiibuh.ws

import io.github.huiibuh.db.tables.TBooks
import io.github.huiibuh.db.tables.TAuthors
import io.github.huiibuh.db.tables.TSeries
import io.ktor.routing.*


fun Route.registerUpdateRoutes(path: String = "ws") {
    route(path) {
        updateForTables(
            TAuthors,
            TBooks,
            TSeries,
        )
    }
}


