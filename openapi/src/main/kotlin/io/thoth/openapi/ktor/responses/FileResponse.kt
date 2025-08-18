@file:Suppress("UnusedReceiverParameter", "unused")

package io.thoth.openapi.ktor.responses

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondFile
import io.ktor.server.routing.RoutingContext
import io.thoth.openapi.ktor.errors.ErrorResponse
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

class FileResponse(
    val path: Path,
) : BaseResponse {
    constructor(path: String) : this(Path.of(path))

    init {
        if (!path.exists() && path.isRegularFile()) {
            throw ErrorResponse(HttpStatusCode.NotFound, "Could not find file $path")
        }
    }

    override suspend fun respond(call: ApplicationCall) {
        call.respondFile(path.toFile())
    }
}

fun RoutingContext.fileResponse(path: Path): FileResponse = FileResponse(path)

fun RoutingContext.fileResponse(path: String): FileResponse = FileResponse(path)
