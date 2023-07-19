package io.thoth.openapi.ktor.responses

import io.ktor.server.application.*

abstract class BaseResponse {

    abstract suspend fun respond(call: ApplicationCall)
}
