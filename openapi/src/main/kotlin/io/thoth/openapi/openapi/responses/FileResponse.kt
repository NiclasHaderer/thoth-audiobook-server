package io.thoth.openapi.openapi.responses

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.thoth.openapi.openapi.RouteHandler
import io.thoth.openapi.openapi.errors.ErrorResponse
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

class FileResponse(val path: Path) : BaseResponse() {
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

fun RouteHandler.fileResponse(path: Path): FileResponse {
    return FileResponse(path)
}

fun RouteHandler.fileResponse(path: String): FileResponse {
    return FileResponse(path)
}
