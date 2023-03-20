package io.thoth.openapi.responses

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.thoth.openapi.RouteHandler

class RedirectResponse(val url: String) : BaseResponse() {
    override suspend fun respond(call: ApplicationCall) {
        call.respondRedirect(url)
    }
}

fun RouteHandler.redirectResponse(url: String): RedirectResponse {
    return RedirectResponse(url)
}
