package io.thoth.server.api

import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.server.response.header
import io.ktor.server.routing.Routing
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.openapi.ktor.get
import io.thoth.openapi.ktor.responses.BinaryResponse
import io.thoth.openapi.ktor.responses.FileResponse
import io.thoth.openapi.ktor.responses.binaryResponse
import io.thoth.openapi.ktor.responses.fileResponse
import io.thoth.server.database.tables.ImageEntity
import io.thoth.server.database.tables.TrackEntity
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

fun Routing.audioRouting() {
    get<Api.Files.Audio.Id, FileResponse> { (id) ->
        val track = transaction { TrackEntity.findById(id)?.path } ?: throw ErrorResponse.notFound("Track", id)
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
            val imageBlob = ImageEntity.findById(id)?.blob ?: throw ErrorResponse.notFound("Image", id)
            binaryResponse(imageBlob.bytes)
        }
    }
}
