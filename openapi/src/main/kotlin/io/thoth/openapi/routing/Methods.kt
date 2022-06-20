package io.thoth.openapi.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import io.thoth.openapi.responses.BinaryResponse
import io.thoth.openapi.responses.FileResponse
import io.thoth.openapi.responses.RedirectResponse
import io.thoth.openapi.serverError
import io.ktor.server.resources.handle as rHandle

typealias RouteHandler = PipelineContext<Unit, ApplicationCall>

// TODO check for nullability of generics
inline fun <reified PARAMS : Any, reified RESPONSE> Route.handleNoBody(
    method: HttpMethod, noinline callback: suspend RouteHandler.(params: PARAMS) -> RESPONSE
): Route {
    lateinit var builtRoute: Route

    if (PARAMS::class == Unit::class) {
        builtRoute = method(method) {
            handle {
                val response: RESPONSE = this.callback(Unit as PARAMS)

                // Catch some edge cases which will be handled differently
                when (response) {
                    is FileResponse -> return@handle call.respondFile(response.path.toFile())
                    is BinaryResponse -> return@handle call.respondBytes(response.bytes)
                    is RedirectResponse -> return@handle call.respondRedirect(response.url)
                }

                if (RESPONSE::class == Unit::class) {
                    call.respond("")
                } else {
                    call.respond(response ?: "")
                }
            }
        }
    } else {
        resource<PARAMS> {
            builtRoute = method(method) {
                rHandle<PARAMS> { params ->
                    val response: RESPONSE = this.callback(params)

                    when (response) {
                        is FileResponse -> return@rHandle call.respondFile(response.path.toFile())
                        is BinaryResponse -> return@rHandle call.respondBytes(response.bytes)
                        is RedirectResponse -> return@rHandle call.respondRedirect(response.url)
                    }


                    if (RESPONSE::class == Unit::class) {
                        call.respond("")
                    } else {
                        call.respond(response ?: "")
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


                when (response) {
                    is FileResponse -> return@handle call.respondFile(response.path.toFile())
                    is BinaryResponse -> return@handle call.respondBytes(response.bytes)
                    is RedirectResponse -> return@handle call.respondRedirect(response.url)
                }

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

                    when (response) {
                        is FileResponse -> return@rHandle call.respondFile(response.path.toFile())
                        is BinaryResponse -> return@rHandle call.respondBytes(response.bytes)
                        is RedirectResponse -> return@rHandle call.respondRedirect(response.url)
                    }

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

inline fun <reified RESPONSE> Route.get(noinline callback: suspend RouteHandler.() -> RESPONSE) {
    handleNoBody<Unit, RESPONSE>(HttpMethod.Get) {
        callback()
    }
}

inline fun <reified PARAMS : Any, reified RESPONSE : Any> Route.get(
    path: String,
    noinline callback: suspend RouteHandler.(params: PARAMS) -> RESPONSE
) {
    route(path) {
        handleNoBody(HttpMethod.Get, callback)
    }
}

inline fun <reified RESPONSE : Any> Route.get(path: String, noinline callback: suspend RouteHandler.() -> RESPONSE) {
    route(path) {
        handleNoBody<Unit, RESPONSE>(HttpMethod.Get) {
            callback()
        }
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

inline fun <reified PARAMS : Any, reified RESPONSE : Any> Route.head(
    path: String,
    noinline callback: suspend RouteHandler.(params: PARAMS) -> RESPONSE
) {
    route(path) {
        handleNoBody(HttpMethod.Head, callback)
    }
}

inline fun <reified RESPONSE : Any> Route.head(path: String, noinline callback: suspend RouteHandler.() -> RESPONSE) {
    route(path) {
        handleNoBody<Unit, RESPONSE>(HttpMethod.Head) {
            callback()
        }
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

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.post(
    path: String,
    noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE
) {
    route(path) {
        handleBody(HttpMethod.Post, callback)
    }
}

inline fun <reified BODY : Any, reified RESPONSE : Any> Route.post(
    path: String,
    noinline callback: suspend RouteHandler.(body: BODY) -> RESPONSE
) {
    route(path) {
        handleBody<Unit, BODY, RESPONSE>(HttpMethod.Post) { _, body ->
            callback(body)
        }
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

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.put(
    path: String,
    noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE
) {
    route(path) {
        handleBody(HttpMethod.Put, callback)
    }
}


inline fun <reified BODY : Any, reified RESPONSE : Any> Route.put(
    path: String,
    noinline callback: suspend RouteHandler.(body: BODY) -> RESPONSE
) {
    route(path) {
        handleBody<Unit, BODY, RESPONSE>(HttpMethod.Put) { _, body ->
            callback(body)
        }
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

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.delete(
    path: String,
    noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE
) {
    route(path) {
        handleBody(HttpMethod.Delete, callback)
    }
}

inline fun <reified BODY : Any, reified RESPONSE : Any> Route.delete(
    path: String,
    noinline callback: suspend RouteHandler.(body: BODY) -> RESPONSE
) {
    route(path) {
        handleBody<Unit, BODY, RESPONSE>(HttpMethod.Delete) { _, body ->
            callback(body)
        }
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

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.options(
    path: String,
    noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE
) {
    route(path) {
        handleBody(HttpMethod.Options, callback)
    }
}

inline fun <reified BODY : Any, reified RESPONSE : Any> Route.options(
    path: String,
    noinline callback: suspend RouteHandler.(body: BODY) -> RESPONSE
) {
    route(path) {
        handleBody<Unit, BODY, RESPONSE>(HttpMethod.Options) { _, body ->
            callback(body)
        }
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

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.patch(
    path: String,
    noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE
) {
    route(path) {
        handleBody(HttpMethod.Patch, callback)
    }
}

inline fun <reified BODY : Any, reified RESPONSE : Any> Route.patch(
    path: String,
    noinline callback: suspend RouteHandler.(body: BODY) -> RESPONSE
) {
    route(path) {
        handleBody<Unit, BODY, RESPONSE>(HttpMethod.Patch) { _, body ->
            callback(body)
        }
    }
}
