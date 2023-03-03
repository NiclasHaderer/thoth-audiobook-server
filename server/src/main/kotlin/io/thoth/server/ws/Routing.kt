package io.thoth.server.ws

import io.ktor.server.routing.*
import io.thoth.database.tables.TAuthors
import io.thoth.database.tables.TBooks
import io.thoth.database.tables.TSeries

fun Route.registerUpdateRoutes(path: String = "ws") {
    route(path) {
        updateForTables(
            TAuthors,
            TBooks,
            TSeries,
        )
    }
}
