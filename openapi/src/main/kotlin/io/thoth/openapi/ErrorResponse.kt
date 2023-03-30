package io.thoth.openapi

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import java.util.*
import mu.KotlinLogging.logger

fun Application.configureStatusPages() {
    val logger = logger {}
    install(StatusPages) {
        exception<ErrorResponse> { call, cause ->
            if (call.response.isSent) return@exception
            call.respond(
                cause.status,
                hashMapOf("error" to cause.message, "status" to cause.status.value, "details" to cause.details),
            )
        }
        exception<Throwable> { call, cause ->
            if (call.response.isSent) return@exception

            call.respond(
                HttpStatusCode.InternalServerError,
                hashMapOf(
                    "error" to cause.message,
                    "status" to HttpStatusCode.InternalServerError.value,
                    "details" to cause.stackTrace,
                ),
            )
            logger.error("Unhandled exception", cause)
        }
        status(*HttpStatusCode.allStatusCodes.filter { !it.isSuccess() }.toTypedArray()) { call, statusCode ->
            if (call.response.isSent) return@status
            call.respond(
                statusCode,
                hashMapOf("error" to statusCode.description, "status" to statusCode.value, "details" to null),
            )
        }
    }
}

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

        fun notImplemented(message: String, details: Any? = null): ErrorResponse {
            return ErrorResponse(HttpStatusCode.NotImplemented, message, details)
        }
    }
}

@Deprecated("Use ErrorResponse instead")
fun PipelineContext<*, *>.serverError(status: HttpStatusCode, message: String, details: Any? = null): Nothing {
    throw ErrorResponse(status, message, details)
}
