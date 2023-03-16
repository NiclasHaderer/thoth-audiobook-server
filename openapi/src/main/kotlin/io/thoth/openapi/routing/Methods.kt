package io.thoth.openapi.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.handle as resourceHandle
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import io.thoth.openapi.SchemaHolder
import io.thoth.openapi.Secured
import io.thoth.openapi.findNearestAnnotation
import io.thoth.openapi.responses.BaseResponse
import io.thoth.openapi.serverError

typealias RouteHandler = PipelineContext<Unit, ApplicationCall>

suspend inline fun <PARAMS : Any, reified BODY : Any, reified RESPONSE> RouteHandler.wrapInnerRequest(
    noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE,
    params: PARAMS,
    method: HttpMethod
) {
    val parsedBody: BODY =
        if (BODY::class == Unit::class) {
            Unit as BODY
        } else {
            try {
                call.receive()
            } catch (e: Exception) {
                val errors = mutableListOf<Exception>()
                errors.add(e)
                while (errors.last().cause != null && errors.last().cause != errors.last()) {
                    errors.add(errors.last().cause as Exception)
                }

                serverError(
                    HttpStatusCode.BadRequest,
                    e.message ?: "body could not be parsed",
                    errors.map { it.message },
                )
            }
        }

    val response: RESPONSE = this.callback(params, parsedBody)

    if (call.response.status() == null) {
        val newStatusCode =
            if (RESPONSE::class == Unit::class) {
                HttpStatusCode.NoContent
            } else if (method == HttpMethod.Post) {
                HttpStatusCode.Created
            } else {
                HttpStatusCode.OK
            }
        call.response.status(newStatusCode)
    }

    if (response is BaseResponse) {
        response.respond(call)
    } else {
        call.respond(response ?: "")
    }
}

inline fun <reified PARAMS : Any, reified RESPONSE> Route.wrapRequest(
    method: HttpMethod,
    noinline callback: suspend RouteHandler.(params: PARAMS) -> RESPONSE
) = wrapRequest<PARAMS, Unit, RESPONSE>(method) { params, _ -> callback(params) }

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE> Route.wrapRequest(
    method: HttpMethod,
    noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE
) {
    SchemaHolder.addRouteToApi<PARAMS, BODY, RESPONSE>(fullPath(PARAMS::class), method)

    val secured = PARAMS::class.findNearestAnnotation<Secured>()
    if (secured != null) {
        authenticate(secured.name) {
            if (PARAMS::class == Unit::class) {
                method(method) { handle { wrapInnerRequest(callback, Unit as PARAMS, method) } }
            } else {
                resource<PARAMS> {
                    method(method) { resourceHandle<PARAMS> { params -> wrapInnerRequest(callback, params, method) } }
                }
            }
        }
    } else {
        if (PARAMS::class == Unit::class) {
            method(method) { handle { wrapInnerRequest(callback, Unit as PARAMS, method) } }
        } else {
            resource<PARAMS> {
                method(method) { resourceHandle<PARAMS> { params -> wrapInnerRequest(callback, params, method) } }
            }
        }
    }
}

inline fun <reified PARAMS : Any, reified RESPONSE : Any> Route.get(
    noinline callback: suspend RouteHandler.(params: PARAMS) -> RESPONSE
) {
    wrapRequest(HttpMethod.Get, callback)
}

inline fun <reified PARAMS : Any, reified RESPONSE : Any> Route.head(
    noinline callback: suspend RouteHandler.(params: PARAMS) -> RESPONSE
) {
    wrapRequest(HttpMethod.Head, callback)
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.post(
    noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE
) {
    wrapRequest(HttpMethod.Post, callback)
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.put(
    noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE
) {
    wrapRequest(HttpMethod.Put, callback)
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.delete(
    noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE
) {
    wrapRequest(HttpMethod.Delete, callback)
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.options(
    noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE
) {
    wrapRequest(HttpMethod.Options, callback)
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.patch(
    noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE
) {
    wrapRequest(HttpMethod.Patch, callback)
}
