@file:Suppress("UnusedReceiverParameter", "unused")

package io.thoth.openapi.ktor.responses

import io.ktor.http.ContentType
import io.ktor.http.defaultForPath
import io.ktor.server.application.ApplicationCall
import io.ktor.server.http.content.LocalFileContent
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import io.thoth.openapi.ktor.errors.ErrorResponse
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.pathString

class FileResponse(
    val path: Path,
    val contentType: ContentType? = null,
) : BaseResponse {
    constructor(path: String, contentType: ContentType? = null) : this(Path.of(path), contentType)

    init {
        if (!path.exists() || !path.isRegularFile()) {
            throw ErrorResponse.notFound("file", path.pathString)
        }
    }

    override suspend fun respond(call: ApplicationCall) {
        call.respond(LocalFileContent(path.toFile(), contentType ?: ContentType.defaultForPath(path)))
    }
}

fun RoutingContext.fileResponse(
    path: Path,
    contentType: ContentType? = null,
): FileResponse = FileResponse(path, contentType)

fun RoutingContext.fileResponse(
    path: String,
    contentType: ContentType? = null,
): FileResponse = FileResponse(path, contentType)
