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


fun Route.registerImageRouting(route: String = "image") {
    route(route) {
        imageRouting()
    }
}

internal fun Route.imageRouting() {
    get<ImageId, BinaryResponse> { image ->
        val imageBlob = transaction { Image.getById(image.id)?.blob } ?: serverError(
            HttpStatusCode.NotFound,
            "Could not find image"
        )
        binaryResponse(imageBlob)
    }
}



