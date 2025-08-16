@file:Suppress("UnusedReceiverParameter")

package io.thoth.openapi.ktor.responses

import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondBytes
import io.thoth.openapi.ktor.RouteHandler

class BinaryResponse(
    val bytes: ByteArray,
) : BaseResponse {
    override suspend fun respond(call: ApplicationCall) {
        call.respondBytes(bytes)
    }
}

fun RouteHandler.binaryResponse(byteArray: ByteArray): BinaryResponse = BinaryResponse(byteArray)
