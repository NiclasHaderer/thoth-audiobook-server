package io.thoth.openapi.ktor.responses

import io.ktor.server.application.ApplicationCall

interface BaseResponse {
    suspend fun respond(call: ApplicationCall)
}
