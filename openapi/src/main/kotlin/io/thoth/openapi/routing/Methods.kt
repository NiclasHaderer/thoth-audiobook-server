package io.thoth.openapi.routing

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.handle as resourceHandle
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import io.thoth.common.extensions.fullPath
import io.thoth.openapi.SchemaHolder
import io.thoth.openapi.responses.BaseResponse
import io.thoth.openapi.serverError

typealias RouteHandler = PipelineContext<Unit, ApplicationCall>

suspend inline fun <PARAMS : Any, reified BODY : Any, reified RESPONSE> RouteHandler.wrapInnerRequest(
    noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE,
    params: PARAMS,
    method: HttpMethod
) {
    val parsedBody: BODY =
        if (BODY::class === Unit::class) {
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
): Route {
    lateinit var builtRoute: Route

    val res = PARAMS::class.annotations.find { a -> a is Resource } as? Resource
    val endPath = res?.path ?: ""
    SchemaHolder.addRouteToApi(fullPath, method, BODY::class, PARAMS::class, RESPONSE::class)

    // Redirect different trailing / to the same route
    if (endPath.endsWith("/")) {
        route(endPath.slice(0..endPath.length - 2)) {
            method(method) {
                handle {
                    val uri =
                        URLBuilder(call.request.uri)
                            .also { it.encodedPath = it.encodedPath + "/" }
                            .toString()
                            .replace("https?://".toRegex(), "")
                    call.respondRedirect(uri, true)
                }
            }
        }
    } else {
        route("$endPath/") {
            method(method) {
                handle {
                    val uri =
                        URLBuilder(call.request.uri)
                            .also { it.encodedPath = it.encodedPath.slice(0..it.encodedPath.length - 2) }
                            .toString()
                            .replace("https?://".toRegex(), "")
                    call.respondRedirect(uri, true)
                }
            }
        }
    }

    if (PARAMS::class == Unit::class) {
        builtRoute = method(method) { handle { wrapInnerRequest(callback, Unit as PARAMS, method) } }
    } else {
        resource<PARAMS> {
            builtRoute =
                method(method) { resourceHandle<PARAMS> { params -> wrapInnerRequest(callback, params, method) } }
        }
    }
    return builtRoute
}

inline fun <reified PARAMS : Any, reified RESPONSE : Any> Route.get(
    noinline callback: suspend RouteHandler.(params: PARAMS) -> RESPONSE
) {
    wrapRequest(HttpMethod.Get, callback)
}

inline fun <reified RESPONSE> Route.get(noinline callback: suspend RouteHandler.() -> RESPONSE) {
    wrapRequest<Unit, RESPONSE>(HttpMethod.Get) { callback() }
}

inline fun <reified PARAMS : Any, reified RESPONSE : Any> Route.get(
    path: String,
    noinline callback: suspend RouteHandler.(params: PARAMS) -> RESPONSE
) {
    route(path) { wrapRequest(HttpMethod.Get, callback) }
}

inline fun <reified RESPONSE : Any> Route.get(path: String, noinline callback: suspend RouteHandler.() -> RESPONSE) {
    route(path) { wrapRequest<Unit, RESPONSE>(HttpMethod.Get) { callback() } }
}

inline fun <reified PARAMS : Any, reified RESPONSE : Any> Route.head(
    noinline callback: suspend RouteHandler.(params: PARAMS) -> RESPONSE
) {
    wrapRequest(HttpMethod.Head, callback)
}

inline fun <reified RESPONSE : Any> Route.head(noinline callback: suspend RouteHandler.() -> RESPONSE) {
    wrapRequest<Unit, RESPONSE>(HttpMethod.Head) { callback() }
}

inline fun <reified PARAMS : Any, reified RESPONSE : Any> Route.head(
    path: String,
    noinline callback: suspend RouteHandler.(params: PARAMS) -> RESPONSE
) {
    route(path) { wrapRequest(HttpMethod.Head, callback) }
}

inline fun <reified RESPONSE : Any> Route.head(path: String, noinline callback: suspend RouteHandler.() -> RESPONSE) {
    route(path) { wrapRequest<Unit, RESPONSE>(HttpMethod.Head) { callback() } }
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.post(
    noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE
) {
    wrapRequest(HttpMethod.Post, callback)
}

inline fun <reified BODY : Any, reified RESPONSE : Any> Route.post(
    noinline callback: suspend RouteHandler.(body: BODY) -> RESPONSE
) {
    wrapRequest<Unit, BODY, RESPONSE>(HttpMethod.Post) { _, body -> callback(body) }
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.post(
    path: String,
    noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE
) {
    route(path) { wrapRequest(HttpMethod.Post, callback) }
}

inline fun <reified BODY : Any, reified RESPONSE : Any> Route.post(
    path: String,
    noinline callback: suspend RouteHandler.(body: BODY) -> RESPONSE
) {
    route(path) { wrapRequest<Unit, BODY, RESPONSE>(HttpMethod.Post) { _, body -> callback(body) } }
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.put(
    noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE
) {
    wrapRequest(HttpMethod.Put, callback)
}

inline fun <reified BODY : Any, reified RESPONSE : Any> Route.put(
    noinline callback: suspend RouteHandler.(body: BODY) -> RESPONSE
) {
    wrapRequest<Unit, BODY, RESPONSE>(HttpMethod.Put) { _, body -> callback(body) }
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.put(
    path: String,
    noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE
) {
    route(path) { wrapRequest(HttpMethod.Put, callback) }
}

inline fun <reified BODY : Any, reified RESPONSE : Any> Route.put(
    path: String,
    noinline callback: suspend RouteHandler.(body: BODY) -> RESPONSE
) {
    route(path) { wrapRequest<Unit, BODY, RESPONSE>(HttpMethod.Put) { _, body -> callback(body) } }
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.delete(
    noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE
) {
    wrapRequest(HttpMethod.Delete, callback)
}

inline fun <reified BODY : Any, reified RESPONSE : Any> Route.delete(
    noinline callback: suspend RouteHandler.(body: BODY) -> RESPONSE
) {
    wrapRequest<Unit, BODY, RESPONSE>(HttpMethod.Delete) { _, body -> callback(body) }
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.delete(
    path: String,
    noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE
) {
    route(path) { wrapRequest(HttpMethod.Delete, callback) }
}

inline fun <reified BODY : Any, reified RESPONSE : Any> Route.delete(
    path: String,
    noinline callback: suspend RouteHandler.(body: BODY) -> RESPONSE
) {
    route(path) { wrapRequest<Unit, BODY, RESPONSE>(HttpMethod.Delete) { _, body -> callback(body) } }
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.options(
    noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE
) {
    wrapRequest(HttpMethod.Options, callback)
}

inline fun <reified BODY : Any, reified RESPONSE : Any> Route.options(
    noinline callback: suspend RouteHandler.(body: BODY) -> RESPONSE
) {
    wrapRequest<Unit, BODY, RESPONSE>(HttpMethod.Options) { _, body -> callback(body) }
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.options(
    path: String,
    noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE
) {
    route(path) { wrapRequest(HttpMethod.Options, callback) }
}

inline fun <reified BODY : Any, reified RESPONSE : Any> Route.options(
    path: String,
    noinline callback: suspend RouteHandler.(body: BODY) -> RESPONSE
) {
    route(path) { wrapRequest<Unit, BODY, RESPONSE>(HttpMethod.Options) { _, body -> callback(body) } }
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.patch(
    noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE
) {
    wrapRequest(HttpMethod.Patch, callback)
}

inline fun <reified BODY : Any, reified RESPONSE : Any> Route.patch(
    noinline callback: suspend RouteHandler.(body: BODY) -> RESPONSE
) {
    wrapRequest<Unit, BODY, RESPONSE>(HttpMethod.Patch) { _, body -> callback(body) }
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.patch(
    path: String,
    noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE
) {
    route(path) { wrapRequest(HttpMethod.Patch, callback) }
}

inline fun <reified BODY : Any, reified RESPONSE : Any> Route.patch(
    path: String,
    noinline callback: suspend RouteHandler.(body: BODY) -> RESPONSE
) {
    route(path) { wrapRequest<Unit, BODY, RESPONSE>(HttpMethod.Patch) { _, body -> callback(body) } }
}
