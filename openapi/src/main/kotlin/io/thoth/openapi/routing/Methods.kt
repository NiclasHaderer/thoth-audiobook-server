package io.thoth.openapi.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import io.thoth.openapi.serverError
import io.ktor.server.resources.handle as rHandle

typealias RouteHandler = PipelineContext<Unit, ApplicationCall>

inline fun <reified PARAMS : Any, reified RESPONSE : Any> Route.handleNoBody(
    method: HttpMethod, noinline callback: suspend RouteHandler.(params: PARAMS) -> RESPONSE
): Route {
    lateinit var builtRoute: Route

    if (PARAMS::class == Unit::class) {
        builtRoute = method(method) {
            handle {
                val response: RESPONSE = this.callback(Unit as PARAMS)
                if (RESPONSE::class == Unit::class) {
                    call.respond("")
                } else {
                    call.respond(response)
                }
            }
        }
    } else {
        resource<PARAMS> {
            builtRoute = method(method) {
                rHandle<PARAMS> { params ->
                    val response: RESPONSE = this.callback(params)
                    if (RESPONSE::class == Unit::class) {
                        call.respond("")
                    } else {
                        call.respond(response)
                    }
                }
            }
        }
    }

    return builtRoute
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.handleBody(
    method: HttpMethod,
    noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE
): Route {
    lateinit var builtRoute: Route


    if (PARAMS::class == Unit::class) {
        builtRoute = method(method) {
            handle {
                val parsedBody = try {
                    call.receive<BODY>()
                } catch (e: ContentTransformationException) {
                    serverError(HttpStatusCode.BadRequest, e.message ?: "body could not be parsed")
                }
                val response: RESPONSE = this.callback(Unit as PARAMS, parsedBody)
                if (RESPONSE::class == Unit::class) {
                    call.respond("")
                } else {
                    call.respond(response)
                }
            }
        }
    } else {
        resource<PARAMS> {
            builtRoute = method(method) {
                rHandle<PARAMS> { params ->

                    val parsedBody = try {
                        call.receive<BODY>()
                    } catch (e: ContentTransformationException) {
                        serverError(HttpStatusCode.BadRequest, e.message ?: "body could not be parsed")
                    }
                    val response: RESPONSE = this.callback(params, parsedBody)
                    if (RESPONSE::class == Unit::class) {
                        call.respond("")
                    } else {
                        call.respond(response)
                    }
                }
            }
        }
    }

    return builtRoute
}

inline fun <reified PARAMS : Any, reified RESPONSE : Any> Route.get(noinline callback: suspend RouteHandler.(params: PARAMS) -> RESPONSE) {
    handleNoBody(HttpMethod.Get, callback)
}

inline fun <reified RESPONSE : Any> Route.get(noinline callback: suspend RouteHandler.() -> RESPONSE) {
    handleNoBody<Unit, RESPONSE>(HttpMethod.Get) {
        callback()
    }
}

inline fun <reified PARAMS : Any, reified RESPONSE : Any> Route.head(noinline callback: suspend RouteHandler.(params: PARAMS) -> RESPONSE) {
    handleNoBody(HttpMethod.Head, callback)
}

inline fun <reified RESPONSE : Any> Route.head(noinline callback: suspend RouteHandler.() -> RESPONSE) {
    handleNoBody<Unit, RESPONSE>(HttpMethod.Head) {
        callback()
    }
}


inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.post(noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE) {
    handleBody(HttpMethod.Post, callback)
}

inline fun <reified BODY : Any, reified RESPONSE : Any> Route.post(noinline callback: suspend RouteHandler.(body: BODY) -> RESPONSE) {
    handleBody<Unit, BODY, RESPONSE>(HttpMethod.Post) { _, body ->
        callback(body)
    }
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.put(noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE) {
    handleBody(HttpMethod.Put, callback)
}


inline fun <reified BODY : Any, reified RESPONSE : Any> Route.put(noinline callback: suspend RouteHandler.(body: BODY) -> RESPONSE) {
    handleBody<Unit, BODY, RESPONSE>(HttpMethod.Put) { _, body ->
        callback(body)
    }
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.delete(noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE) {
    handleBody(HttpMethod.Delete, callback)
}

inline fun <reified BODY : Any, reified RESPONSE : Any> Route.delete(noinline callback: suspend RouteHandler.(body: BODY) -> RESPONSE) {
    handleBody<Unit, BODY, RESPONSE>(HttpMethod.Delete) { _, body ->
        callback(body)
    }
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.options(noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE) {
    handleBody(HttpMethod.Options, callback)
}

inline fun <reified BODY : Any, reified RESPONSE : Any> Route.options(noinline callback: suspend RouteHandler.(body: BODY) -> RESPONSE) {
    handleBody<Unit, BODY, RESPONSE>(HttpMethod.Options) { _, body ->
        callback(body)
    }
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.patch(noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE) {
    handleBody(HttpMethod.Patch, callback)
}

inline fun <reified BODY : Any, reified RESPONSE : Any> Route.patch(noinline callback: suspend RouteHandler.(body: BODY) -> RESPONSE) {
    handleBody<Unit, BODY, RESPONSE>(HttpMethod.Patch) { _, body ->
        callback(body)
    }
}
