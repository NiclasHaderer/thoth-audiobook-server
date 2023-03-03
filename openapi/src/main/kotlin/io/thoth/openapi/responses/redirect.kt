package io.thoth.openapi.responses

import io.thoth.openapi.routing.RouteHandler

class RedirectResponse(val url: String)

fun RouteHandler.redirectResponse(url: String): RedirectResponse {
    return RedirectResponse(url)
}
