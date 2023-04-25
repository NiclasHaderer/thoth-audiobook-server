package io.thoth.generators.openapi.errors

import io.ktor.http.*
import java.util.*

class ErrorResponse internal constructor(val status: HttpStatusCode, message: String, val details: Any? = null) :
    Exception(message) {
    companion object {
        fun notFound(thing: String, id: UUID, details: Any? = null): ErrorResponse {
            return notFound(thing, id.toString(), details)
        }

        fun notFound(thing: String, id: String, details: Any? = null): ErrorResponse {
            return ErrorResponse(HttpStatusCode.NotFound, "$thing with ID:$id not found", details)
        }

        fun userError(message: String, details: Any? = null): ErrorResponse {
            return ErrorResponse(HttpStatusCode.BadRequest, message, details)
        }

        fun unauthorized(resource: String, details: Any? = null): ErrorResponse {
            return ErrorResponse(HttpStatusCode.Unauthorized, "Unauthorized to access $resource", details)
        }

        fun notImplemented(message: String, details: Any? = null): ErrorResponse {
            return ErrorResponse(HttpStatusCode.NotImplemented, message, details)
        }

        fun internalError(message: String, details: Any? = null): ErrorResponse {
            return ErrorResponse(HttpStatusCode.InternalServerError, message, details)
        }
    }
}
