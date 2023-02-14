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
            call.respond(cause.status, hashMapOf("error" to cause.message))
        }
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                hashMapOf(
                    "error" to cause.message,
                    "trace" to cause.stackTrace.joinToString("\n")
                )
            )
            logger.error("Unhandled exception", cause)
        }
    }
}


class ErrorResponse(val status: HttpStatusCode, message: String) : Exception(message)

fun PipelineContext<*, *>.serverError(status: HttpStatusCode, message: String): Nothing {
    throw ErrorResponse(status, message)
}
