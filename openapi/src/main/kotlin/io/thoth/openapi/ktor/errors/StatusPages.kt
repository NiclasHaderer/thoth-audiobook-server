package io.thoth.openapi.ktor.errors

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
                hashMapOf("error" to cause.error, "status" to cause.status.value, "details" to cause.details),
            )
        }

        exception<Throwable>(formatException(HttpStatusCode.InternalServerError) { logger.error(it) })
        exception<BadRequestException>(formatException(HttpStatusCode.BadRequest))
        exception<MissingRequestParameterException>(formatException(HttpStatusCode.BadRequest))
        exception<ParameterConversionException>(formatException(HttpStatusCode.BadRequest))
        exception<ContentTransformationException>(formatException(HttpStatusCode.InternalServerError))
        exception<CannotTransformContentToTypeException>(formatException(HttpStatusCode.UnsupportedMediaType))
        exception<UnsupportedMediaTypeException>(formatException(HttpStatusCode.UnsupportedMediaType))
        exception<MissingRequestParameterException>(formatException(HttpStatusCode.BadRequest))

        val statuses = HttpStatusCode.allStatusCodes.filter { it.value >= 400 }.toTypedArray()
        status(*statuses) { statusCode ->
            call.respond(
                statusCode,
                hashMapOf("error" to statusCode.description, "status" to statusCode.value, "details" to null),
            )
        }
    }
}
