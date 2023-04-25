package io.thoth.generators.openapi.responses

import io.ktor.server.application.*

typealias SchemaName = String?

abstract class BaseResponse {

    abstract suspend fun respond(call: ApplicationCall)
}
