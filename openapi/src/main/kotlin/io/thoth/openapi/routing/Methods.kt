package io.thoth.openapi.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import io.thoth.common.extensions.fullPath
import io.thoth.openapi.SchemaHolder
import io.thoth.openapi.responses.BinaryResponse
import io.thoth.openapi.responses.FileResponse
import io.thoth.openapi.responses.RedirectResponse
import io.thoth.openapi.serverError
import io.ktor.server.resources.handle as rHandle

typealias RouteHandler = PipelineContext<Unit, ApplicationCall>

suspend inline fun <PARAMS : Any, reified BODY : Any, reified RESPONSE> RouteHandler.wrapInnerRequest(
    noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE,
    params: PARAMS
) {
    val parsedBody: BODY = if (BODY::class === Unit::class) {
        Unit as BODY
    } else {
        try {
            call.receive()
        } catch (e: ContentTransformationException) {
            serverError(HttpStatusCode.BadRequest, e.message ?: "body could not be parsed")
        }
    }

    val response: RESPONSE = this.callback(params, parsedBody)

    // Catch some edge cases which will be handled differently
    when (response) {
        is FileResponse -> return call.respondFile(response.path.toFile())
        is BinaryResponse -> return call.respondBytes(response.bytes)
        is RedirectResponse -> return call.respondRedirect(response.url)
    }

    if (RESPONSE::class == Unit::class) {
        call.respond("")
    } else {
        call.respond(response ?: "")
    }
}

inline fun <reified PARAMS : Any, reified RESPONSE> Route.wrapRequest(
    method: HttpMethod, noinline callback: suspend RouteHandler.(params: PARAMS) -> RESPONSE
) = wrapRequest<PARAMS, Unit, RESPONSE>(method) { params, _ -> callback(params) }

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE> Route.wrapRequest(
    method: HttpMethod, noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE
): Route {
    lateinit var builtRoute: Route

    // TODO status code
    SchemaHolder.addRouteToApi(fullPath, method, BODY::class, PARAMS::class, RESPONSE::class, HttpStatusCode.OK)

    if (PARAMS::class == Unit::class) {
        builtRoute = method(method) {
            handle { wrapInnerRequest(callback, Unit as PARAMS) }
        }
    } else {
        resource<PARAMS> {
            builtRoute = method(method) {
                rHandle<PARAMS> { params -> wrapInnerRequest(callback, params) }
            }
        }
    }

    return builtRoute
}

inline fun <reified PARAMS : Any, reified RESPONSE : Any> Route.get(noinline callback: suspend RouteHandler.(params: PARAMS) -> RESPONSE) {
    wrapRequest(HttpMethod.Get, callback)
}

inline fun <reified RESPONSE> Route.get(noinline callback: suspend RouteHandler.() -> RESPONSE) {
    wrapRequest<Unit, RESPONSE>(HttpMethod.Get) {
        callback()
    }
}

inline fun <reified PARAMS : Any, reified RESPONSE : Any> Route.get(
    path: String,
    noinline callback: suspend RouteHandler.(params: PARAMS) -> RESPONSE
) {
    route(path) {
        wrapRequest(HttpMethod.Get, callback)
    }
}

inline fun <reified RESPONSE : Any> Route.get(
    path: String,
    noinline callback: suspend RouteHandler.() -> RESPONSE
) {
    route(path) {
        wrapRequest<Unit, RESPONSE>(HttpMethod.Get) {
            callback()
        }
    }
}

inline fun <reified PARAMS : Any, reified RESPONSE : Any> Route.head(noinline callback: suspend RouteHandler.(params: PARAMS) -> RESPONSE) {
    wrapRequest(HttpMethod.Head, callback)
}

inline fun <reified RESPONSE : Any> Route.head(noinline callback: suspend RouteHandler.() -> RESPONSE) {
    wrapRequest<Unit, RESPONSE>(HttpMethod.Head) {
        callback()
    }
}

inline fun <reified PARAMS : Any, reified RESPONSE : Any> Route.head(
    path: String,
    noinline callback: suspend RouteHandler.(params: PARAMS) -> RESPONSE
) {
    route(path) {
        wrapRequest(HttpMethod.Head, callback)
    }
}

inline fun <reified RESPONSE : Any> Route.head(
    path: String,
    noinline callback: suspend RouteHandler.() -> RESPONSE
) {
    route(path) {
        wrapRequest<Unit, RESPONSE>(HttpMethod.Head) {
            callback()
        }
    }
}


inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.post(noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE) {
    wrapRequest(HttpMethod.Post, callback)
}

inline fun <reified BODY : Any, reified RESPONSE : Any> Route.post(noinline callback: suspend RouteHandler.(body: BODY) -> RESPONSE) {
    wrapRequest<Unit, BODY, RESPONSE>(HttpMethod.Post) { _, body ->
        callback(body)
    }
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.post(
    path: String,
    noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE
) {
    route(path) {
        wrapRequest(HttpMethod.Post, callback)
    }
}

inline fun <reified BODY : Any, reified RESPONSE : Any> Route.post(
    path: String,
    noinline callback: suspend RouteHandler.(body: BODY) -> RESPONSE
) {
    route(path) {
        wrapRequest<Unit, BODY, RESPONSE>(HttpMethod.Post) { _, body ->
            callback(body)
        }
    }
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.put(noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE) {
    wrapRequest(HttpMethod.Put, callback)
}


inline fun <reified BODY : Any, reified RESPONSE : Any> Route.put(noinline callback: suspend RouteHandler.(body: BODY) -> RESPONSE) {
    wrapRequest<Unit, BODY, RESPONSE>(HttpMethod.Put) { _, body ->
        callback(body)
    }
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.put(
    path: String,
    noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE
) {
    route(path) {
        wrapRequest(HttpMethod.Put, callback)
    }
}


inline fun <reified BODY : Any, reified RESPONSE : Any> Route.put(
    path: String,
    noinline callback: suspend RouteHandler.(body: BODY) -> RESPONSE
) {
    route(path) {
        wrapRequest<Unit, BODY, RESPONSE>(HttpMethod.Put) { _, body ->
            callback(body)
        }
    }
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.delete(noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE) {
    wrapRequest(HttpMethod.Delete, callback)
}

inline fun <reified BODY : Any, reified RESPONSE : Any> Route.delete(noinline callback: suspend RouteHandler.(body: BODY) -> RESPONSE) {
    wrapRequest<Unit, BODY, RESPONSE>(HttpMethod.Delete) { _, body ->
        callback(body)
    }
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.delete(
    path: String,
    noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE
) {
    route(path) {
        wrapRequest(HttpMethod.Delete, callback)
    }
}

inline fun <reified BODY : Any, reified RESPONSE : Any> Route.delete(
    path: String,
    noinline callback: suspend RouteHandler.(body: BODY) -> RESPONSE
) {
    route(path) {
        wrapRequest<Unit, BODY, RESPONSE>(HttpMethod.Delete) { _, body ->
            callback(body)
        }
    }
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.options(noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE) {
    wrapRequest(HttpMethod.Options, callback)
}

inline fun <reified BODY : Any, reified RESPONSE : Any> Route.options(noinline callback: suspend RouteHandler.(body: BODY) -> RESPONSE) {
    wrapRequest<Unit, BODY, RESPONSE>(HttpMethod.Options) { _, body ->
        callback(body)
    }
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.options(
    path: String,
    noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE
) {
    route(path) {
        wrapRequest(HttpMethod.Options, callback)
    }
}

inline fun <reified BODY : Any, reified RESPONSE : Any> Route.options(
    path: String,
    noinline callback: suspend RouteHandler.(body: BODY) -> RESPONSE
) {
    route(path) {
        wrapRequest<Unit, BODY, RESPONSE>(HttpMethod.Options) { _, body ->
            callback(body)
        }
    }
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.patch(noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE) {
    wrapRequest(HttpMethod.Patch, callback)
}

inline fun <reified BODY : Any, reified RESPONSE : Any> Route.patch(noinline callback: suspend RouteHandler.(body: BODY) -> RESPONSE) {
    wrapRequest<Unit, BODY, RESPONSE>(HttpMethod.Patch) { _, body ->
        callback(body)
    }
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.patch(
    path: String,
    noinline callback: suspend RouteHandler.(params: PARAMS, body: BODY) -> RESPONSE
) {
    route(path) {
        wrapRequest(HttpMethod.Patch, callback)
    }
}

inline fun <reified BODY : Any, reified RESPONSE : Any> Route.patch(
    path: String,
    noinline callback: suspend RouteHandler.(body: BODY) -> RESPONSE
) {
    route(path) {
        wrapRequest<Unit, BODY, RESPONSE>(HttpMethod.Patch) { _, body ->
            callback(body)
        }
    }
}
