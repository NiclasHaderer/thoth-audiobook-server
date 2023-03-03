package io.thoth.openapi

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import mu.KotlinLogging.logger

fun Application.configureStatusPages() {
    val logger = logger {}
    install(StatusPages) {
        exception<ErrorResponse> { call, cause ->
            call.respond(
                cause.status,
                hashMapOf(
                    "error" to cause.message,
                    "status" to cause.status.value,
                    "details" to cause.details
                )
            )
        }
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                hashMapOf(
                    "error" to cause.message,
                    "status" to HttpStatusCode.InternalServerError.value,
                    "trace" to cause.stackTrace
                )
            )
            logger.error("Unhandled exception", cause)
        }
    }
}

class ErrorResponse(val status: HttpStatusCode, message: String, val details: Any? = null) :
    Exception(message)

fun PipelineContext<*, *>.serverError(
    status: HttpStatusCode,
    message: String,
    details: Any? = null
): Nothing {
    throw ErrorResponse(status, message, details)
}
