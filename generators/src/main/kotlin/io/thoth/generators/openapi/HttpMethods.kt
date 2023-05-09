package io.thoth.generators.openapi

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.handle as resourceHandle
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import io.thoth.generators.common.findAnnotationUp
import io.thoth.generators.openapi.errors.ErrorResponse
import io.thoth.generators.openapi.responses.BaseResponse
import kotlin.reflect.full.findAnnotation

typealias RouteHandler = PipelineContext<Unit, ApplicationCall>

suspend inline fun <reified BODY : Any> ApplicationCall.parseBody(): BODY {
    return if (BODY::class == Unit::class) {
        Unit as BODY
    } else {
        try {
            receive()
        } catch (e: Exception) {
            val errors = mutableListOf<Exception>()
            errors.add(e)
            while (errors.last().cause != null && errors.last().cause != errors.last()) {
                errors.add(errors.last().cause as Exception)
            }

            throw ErrorResponse.userError(
                e.message ?: "body could not be parsed",
                errors.map { it.message },
            )
        }
    }
}

suspend inline fun <PARAMS : Any, reified BODY : Any, reified RESPONSE> RouteHandler.wrapHandler(
    noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE,
    params: PARAMS,
) {
    val parsedBody: BODY = call.parseBody()
    val response: RESPONSE = this.callback(params, parsedBody)
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

    OpenApiRouteCollector.addRoute(
        OpenApiRoute.create<PARAMS, BODY, RESPONSE>(method, this),
    )

    // Check if the route should be secured by ktor
    val ignoreSecured = PARAMS::class.findAnnotation<NotSecured>() != null
    val secured = PARAMS::class.findAnnotationUp<Secured>()
    if (secured != null && !ignoreSecured) {
        authenticate(secured.name) {
            if (PARAMS::class == Unit::class) {
                method(method) { handle { wrapHandler(callback, Unit as PARAMS) } }
            } else {
                resource<PARAMS> {
                    method(method) { resourceHandle<PARAMS> { params -> wrapHandler(callback, params) } }
                }
            }
        }
    } else {
        if (PARAMS::class == Unit::class) {
            method(method) { handle { wrapHandler(callback, Unit as PARAMS) } }
        } else {
            resource<PARAMS> { method(method) { resourceHandle<PARAMS> { params -> wrapHandler(callback, params) } } }
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
