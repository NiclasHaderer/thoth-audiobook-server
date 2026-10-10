package io.thoth.server.ws

import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.BookTable
import io.thoth.server.database.tables.SeriesTable

fun Route.registerUpdateRoutes(path: String = "ws") {
    route(path) { updateForTables(AuthorTable, BookTable, SeriesTable) }
}
