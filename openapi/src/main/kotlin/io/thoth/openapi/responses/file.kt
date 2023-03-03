package io.thoth.openapi.responses

import io.ktor.http.*
import io.thoth.openapi.ErrorResponse
import io.thoth.openapi.routing.RouteHandler
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

class FileResponse(val path: Path) {
    constructor(path: String) : this(Path.of(path))

    init {
        if (!path.exists() && path.isRegularFile()) {
            throw ErrorResponse(HttpStatusCode.NotFound, "Could not find file $path")
        }
    }
}

fun RouteHandler.fileResponse(path: Path): FileResponse {
    return FileResponse(path)
}

fun RouteHandler.fileResponse(path: String): FileResponse {
    return FileResponse(path)
}
