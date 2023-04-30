package io.thoth.generators.openapi.errors

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.util.logging.*
import mu.KotlinLogging

private fun <T> formatException(
    statusCode: HttpStatusCode,
    cb: ((cause: T) -> Unit)? = null
): suspend (call: ApplicationCall, cause: T) -> Unit {
    return { call, cause ->
        if (!call.response.isSent) {
            call.respond(
                statusCode,
                hashMapOf("error" to cause.toString(), "status" to statusCode.value, "details" to null),
            )
        }
        cb?.invoke(cause)
    }
}

fun Application.configureStatusPages() {
    val logger = KotlinLogging.logger {}
    install(StatusPages) {
        exception<ErrorResponse> { call, cause ->
            if (call.response.isSent) return@exception
            call.respond(
                cause.status,
                hashMapOf("error" to cause.message, "status" to cause.status.value, "details" to cause.details),
            )
        }

        exception<Throwable>(formatException(HttpStatusCode.InternalServerError) { logger.error(it) })

        exception<BadRequestException>(formatException(HttpStatusCode.BadRequest))
        exception<MissingRequestParameterException>(formatException(HttpStatusCode.BadRequest))

        status(*HttpStatusCode.allStatusCodes.filter { !it.isSuccess() }.toTypedArray()) { call, statusCode ->
            if (call.response.isSent) return@status
            call.respond(
                statusCode,
                hashMapOf("error" to statusCode.description, "status" to statusCode.value, "details" to null),
            )
        }
    }
}
