package io.thoth.openapi.ktor.responses

import io.ktor.server.application.*

interface BaseResponse {
    suspend fun respond(call: ApplicationCall)
}
