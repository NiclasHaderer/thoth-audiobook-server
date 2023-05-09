package io.thoth.server.api

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.thoth.generators.openapi.errors.ErrorResponse
import io.thoth.generators.openapi.get
import io.thoth.generators.openapi.responses.BinaryResponse
import io.thoth.generators.openapi.responses.FileResponse
import io.thoth.generators.openapi.responses.binaryResponse
import io.thoth.generators.openapi.responses.fileResponse
import io.thoth.server.database.tables.Image
import io.thoth.server.database.tables.Track
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import org.jetbrains.exposed.sql.transactions.transaction

fun Routing.audioRouting() {
    get<Api.Files.Audio.Id, FileResponse> { (id) ->
        val track = transaction { Track.findById(id)?.path } ?: throw ErrorResponse.notFound("Track", id)
        val path = Path.of(track)
        if (!path.exists() && path.isRegularFile()) {
            throw ErrorResponse.notFound("File", path.name, "Database out of sync. Please start a rescan.")
        }
        call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, path.name).toString(),
        )
        fileResponse(path)
    }
}

fun Routing.imageRouting() {
    get<Api.Files.Images.Id, BinaryResponse> { (id) ->
        transaction {
            val imageBlob = Image.findById(id)?.blob ?: throw ErrorResponse.notFound("Image", id)
            binaryResponse(imageBlob.bytes)
        }
    }
}
