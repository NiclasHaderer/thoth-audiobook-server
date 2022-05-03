package io.thoth.common.exceptions

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<ErrorResponse> { cause ->
            call.respond(cause.status, hashMapOf("error" to cause.message))
        }
        exception<Throwable> { cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                hashMapOf("error" to cause.message, "trace" to cause.stackTrace)
            )
            throw cause
        }
    }
}


class ErrorResponse(val status: HttpStatusCode, message: String) : Exception(message)
