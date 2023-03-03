package io.thoth.openapi.responses

import io.thoth.openapi.routing.RouteHandler

class BinaryResponse(val bytes: ByteArray)

fun RouteHandler.binaryResponse(byteArray: ByteArray): BinaryResponse {
    return BinaryResponse(byteArray)
}
