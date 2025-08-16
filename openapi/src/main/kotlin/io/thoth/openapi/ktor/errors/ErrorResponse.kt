package io.thoth.openapi.ktor.errors

import io.ktor.http.*
import java.util.*

class ErrorResponse(
    val status: HttpStatusCode,
    val error: String,
    val details: Any? = null,
) : Exception(error) {
    companion object {
        fun notFound(
            thing: String,
            id: UUID,
            details: Any? = null,
        ): ErrorResponse = notFound(thing, id.toString(), details)

        fun notFound(
            thing: String,
            id: Any,
            details: Any? = null,
        ): ErrorResponse = ErrorResponse(HttpStatusCode.NotFound, "$thing with ID:$id not found", details)

        fun userError(
            error: String,
            details: Any? = null,
        ): ErrorResponse = ErrorResponse(HttpStatusCode.BadRequest, error, details)

        fun unauthorized(
            error: String,
            details: Any? = null,
        ): ErrorResponse = ErrorResponse(HttpStatusCode.Unauthorized, error, details)

        fun forbidden(
            action: String,
            resource: String,
            details: Any? = null,
        ): ErrorResponse = ErrorResponse(HttpStatusCode.Forbidden, "Forbidden to $action $resource", details)

        fun notImplemented(
            error: String,
            details: Any? = null,
        ): ErrorResponse = ErrorResponse(HttpStatusCode.NotImplemented, error, details)

        fun internalError(
            error: String,
            details: Any? = null,
        ): ErrorResponse = ErrorResponse(HttpStatusCode.InternalServerError, error, details)
    }
}
