package io.thoth.openapi.responses

import io.ktor.server.application.*

abstract class BaseResponse {
    abstract suspend fun respond(call: ApplicationCall)
}
