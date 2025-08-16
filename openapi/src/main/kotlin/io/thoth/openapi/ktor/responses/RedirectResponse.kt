@file:Suppress("UnusedReceiverParameter", "unused")

package io.thoth.openapi.ktor.responses

import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondRedirect
import io.thoth.openapi.ktor.RouteHandler

class RedirectResponse(
    val url: String,
) : BaseResponse {
    override suspend fun respond(call: ApplicationCall) {
        call.respondRedirect(url)
    }
}

fun RouteHandler.redirectResponse(url: String): RedirectResponse = RedirectResponse(url)
