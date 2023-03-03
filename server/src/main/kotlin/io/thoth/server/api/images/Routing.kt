package io.thoth.server.api.images

import io.ktor.http.*
import io.ktor.server.routing.*
import io.thoth.database.access.getById
import io.thoth.database.tables.Image
import io.thoth.openapi.responses.BinaryResponse
import io.thoth.openapi.responses.binaryResponse
import io.thoth.openapi.routing.get
import io.thoth.openapi.serverError
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.registerImageRouting() =
    route("image") {
        get<ImageId, BinaryResponse> { image ->
            transaction {
                val imageBlob =
                    Image.getById(image.id)?.blob
                        ?: serverError(HttpStatusCode.NotFound, "Could not find image")
                binaryResponse(imageBlob)
            }
        }
    }
