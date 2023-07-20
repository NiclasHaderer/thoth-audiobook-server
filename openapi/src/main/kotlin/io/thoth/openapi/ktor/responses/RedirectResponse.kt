package io.thoth.openapi.ktor.responses

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.thoth.openapi.ktor.RouteHandler

class RedirectResponse(val url: String) : BaseResponse {
    override suspend fun respond(call: ApplicationCall) {
        call.respondRedirect(url)
    }
}

fun RouteHandler.redirectResponse(url: String): RedirectResponse {
    return RedirectResponse(url)
}
