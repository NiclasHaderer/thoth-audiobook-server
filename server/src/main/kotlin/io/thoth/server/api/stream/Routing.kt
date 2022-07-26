package io.thoth.server.api.stream

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.thoth.database.tables.Track
import io.thoth.openapi.responses.FileResponse
import io.thoth.openapi.responses.fileResponse
import io.thoth.openapi.routing.get
import io.thoth.openapi.serverError
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.name


fun Route.registerStreamingRouting(route: String = "audio") {
    route(route) {
        streamingRouting()
    }
}

fun Route.streamingRouting() {
    get<AudioId, FileResponse> { fileId ->
        val track = Track.getById(fileId.id) ?: serverError(
            HttpStatusCode.NotFound,
            "Database out of sync. Please start syncing process."
        )
        val path = Path.of(track.path)
        if (!path.exists() && path.isRegularFile()) {
            serverError(
                HttpStatusCode.NotFound,
                "File does not exist. Database out of sync. Please start syncing process."
            )
        }
        call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, path.name).toString()
        )

        fileResponse(path)
    }
}



