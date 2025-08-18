@file:Suppress("unused")

package io.thoth.openapi.ktor

import io.ktor.http.HttpMethod
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.resources.resource
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.application
import io.ktor.server.routing.method
import io.ktor.server.routing.route
import io.thoth.openapi.common.findAnnotationUp
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.openapi.ktor.plugins.OpenAPIConfigurationKey
import io.thoth.openapi.ktor.responses.BaseResponse
import kotlin.reflect.full.findAnnotation
import io.ktor.server.resources.handle as resourceHandle

suspend inline fun <reified BODY : Any> ApplicationCall.parseBody(): BODY =
    if (BODY::class == Unit::class) {
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

            throw ErrorResponse.userError(e.message ?: "body could not be parsed", errors.map { it.message })
        }
    }

suspend inline fun <PARAMS : Any, reified BODY : Any, reified RESPONSE> RoutingContext.wrapHandler(
    noinline callback: suspend RoutingContext.(params: PARAMS, body: BODY) -> RESPONSE,
    params: PARAMS,
) {
    if (params is BeforeBodyParsing) params.run { beforeBodyParsing() }
    val parsedBody: BODY = call.parseBody()
    if (parsedBody is ValidateObject) parsedBody.run { validateBody() }
    if (params is AfterBodyParsing) params.run { afterBodyParsing() }
    val response: RESPONSE = this.callback(params, parsedBody)
    if (response is BaseResponse) {
        response.respond(call)
    } else {
        call.respond(response ?: "")
    }
    if (params is AfterResponse) params.run { afterResponse() }
}

inline fun <reified PARAMS : Any, reified RESPONSE> Route.wrapRequest(
    method: HttpMethod,
    noinline callback: suspend RoutingContext.(params: PARAMS) -> RESPONSE,
) = wrapRequest<PARAMS, Unit, RESPONSE>(method) { params, _ -> callback(params) }

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE> Route.wrapRequest(
    method: HttpMethod,
    noinline callback: suspend RoutingContext.(params: PARAMS, body: BODY) -> RESPONSE,
) {
    val routeCollector = application.attributes[OpenAPIConfigurationKey].routeCollector

    routeCollector.addRoute(OpenApiRoute.create<PARAMS, BODY, RESPONSE>(method, this))

    // Check if ktor should secure the route
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
    noinline callback: suspend RoutingContext.(params: PARAMS) -> RESPONSE,
) {
    wrapRequest(HttpMethod.Get, callback)
}

inline fun <reified PARAMS : Any, reified RESPONSE : Any> Route.get(
    path: String,
    noinline callback: suspend RoutingContext.(params: PARAMS) -> RESPONSE,
) {
    route(path) { wrapRequest(HttpMethod.Get, callback) }
}

inline fun <reified PARAMS : Any, reified RESPONSE : Any> Route.head(
    noinline callback: suspend RoutingContext.(params: PARAMS) -> RESPONSE,
) {
    wrapRequest(HttpMethod.Head, callback)
}

inline fun <reified PARAMS : Any, reified RESPONSE : Any> Route.head(
    path: String,
    noinline callback: suspend RoutingContext.(params: PARAMS) -> RESPONSE,
) {
    route(path) { wrapRequest(HttpMethod.Head, callback) }
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.post(
    noinline callback: suspend RoutingContext.(params: PARAMS, body: BODY) -> RESPONSE,
) {
    wrapRequest(HttpMethod.Post, callback)
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.post(
    path: String,
    noinline callback: suspend RoutingContext.(params: PARAMS, body: BODY) -> RESPONSE,
) {
    route(path) { wrapRequest(HttpMethod.Post, callback) }
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.put(
    noinline callback: suspend RoutingContext.(params: PARAMS, body: BODY) -> RESPONSE,
) {
    wrapRequest(HttpMethod.Put, callback)
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.put(
    path: String,
    noinline callback: suspend RoutingContext.(params: PARAMS, body: BODY) -> RESPONSE,
) {
    route(path) { wrapRequest(HttpMethod.Put, callback) }
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.delete(
    noinline callback: suspend RoutingContext.(params: PARAMS, body: BODY) -> RESPONSE,
) {
    wrapRequest(HttpMethod.Delete, callback)
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.delete(
    path: String,
    noinline callback: suspend RoutingContext.(params: PARAMS, body: BODY) -> RESPONSE,
) {
    route(path) { wrapRequest(HttpMethod.Delete, callback) }
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.options(
    noinline callback: suspend RoutingContext.(params: PARAMS, body: BODY) -> RESPONSE,
) {
    wrapRequest(HttpMethod.Options, callback)
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.options(
    path: String,
    noinline callback: suspend RoutingContext.(params: PARAMS, body: BODY) -> RESPONSE,
) {
    route(path) { wrapRequest(HttpMethod.Options, callback) }
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.patch(
    noinline callback: suspend RoutingContext.(params: PARAMS, body: BODY) -> RESPONSE,
) {
    wrapRequest(HttpMethod.Patch, callback)
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.patch(
    path: String,
    noinline callback: suspend RoutingContext.(params: PARAMS, body: BODY) -> RESPONSE,
) {
    route(path) { wrapRequest(HttpMethod.Patch, callback) }
}
