package io.thoth.server.api.audiobooks.rescan

import io.ktor.http.*
import io.ktor.server.routing.*
import io.thoth.common.extensions.get
import io.thoth.database.access.toModel
import io.thoth.database.tables.Library
import io.thoth.openapi.routing.post
import io.thoth.openapi.serverError
import io.thoth.server.file.scanner.LibraryScanner
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.registerRescan(path: String = "rescan") =
    route(path) {
        // TODO add a way to stop the scanner
        val scanner = get<LibraryScanner>()

        post<Unit, Unit> { launch { scanner.fullScan() } }

        post<LibraryId, Unit> { (id) ->
            val library =
                transaction { Library.findById(id)?.toModel() }
                    ?: serverError(
                        HttpStatusCode.BadRequest,
                        "Library with id $id not found",
                    )
            launch { scanner.scanLibrary(library) }
        }
    }
