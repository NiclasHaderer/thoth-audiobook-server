package io.thoth.server.api

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.thoth.database.access.getById
import io.thoth.database.tables.Image
import io.thoth.database.tables.Track
import io.thoth.openapi.get
import io.thoth.openapi.responses.BinaryResponse
import io.thoth.openapi.responses.FileResponse
import io.thoth.openapi.responses.binaryResponse
import io.thoth.openapi.responses.fileResponse
import io.thoth.openapi.serverError
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import org.jetbrains.exposed.sql.transactions.transaction

fun Routing.audioRouting() {
    get<Api.Files.Audio.Id, FileResponse> { (id) ->
        val track = transaction { Track.getById(id) } ?: serverError(HttpStatusCode.NotFound, "Track not found.")
        val path = Path.of(track.path)
        if (!path.exists() && path.isRegularFile()) {
            serverError(
                HttpStatusCode.NotFound,
                "File does not exist. Database out of sync. Please start syncing process.",
            )
        }
        call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, path.name).toString(),
        )
        // TODO: Make all custom responses extend base-response and add a method which gets called
        //  and adds the headers/data/etc... to the response
        fileResponse(path)
    }
}

fun Routing.imageRouting() {
    get<Api.Files.Images.Id, BinaryResponse> { (id) ->
        transaction {
            val imageBlob = Image.getById(id)?.blob ?: serverError(HttpStatusCode.NotFound, "Could not find image")
            binaryResponse(imageBlob)
        }
    }
}
