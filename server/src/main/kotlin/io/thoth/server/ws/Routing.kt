package io.thoth.server.ws

import io.ktor.server.routing.*
import io.thoth.server.database.tables.TAuthors
import io.thoth.server.database.tables.TBooks
import io.thoth.server.database.tables.TSeries

fun Route.registerUpdateRoutes(path: String = "ws") {
    route(path) {
        updateForTables(
            TAuthors,
            TBooks,
            TSeries,
        )
    }
}
