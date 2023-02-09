package io.thoth.server.api.images

import io.ktor.http.*
import io.ktor.server.routing.*
import io.thoth.database.access.getById
import io.thoth.database.tables.Image
import io.thoth.openapi.responses.BinaryResponse
import io.thoth.openapi.responses.binaryResponse
import io.thoth.openapi.routing.get
import io.thoth.openapi.serverError


fun Route.registerImageRouting(route: String = "image") {
    route(route) {
        imageRouting()
    }
}

internal fun Route.imageRouting() {
    get<ImageId, BinaryResponse> { image ->
        val imageResponse = Image.getById(image.id) ?: serverError(HttpStatusCode.NotFound, "Could not find image")
        binaryResponse(imageResponse.blob)
    }
}



