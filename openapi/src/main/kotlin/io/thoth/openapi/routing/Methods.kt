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

inline fun <reified PARAMS : Any, reified RESPONSE : Any> Route.handleNoBody(
    method: HttpMethod, noinline callback: suspend PipelineContext<Unit, ApplicationCall>.(params: PARAMS) -> RESPONSE
): Route {
    lateinit var builtRoute: Route
    resource<PARAMS> {
        builtRoute = method(method) {
            rHandle<PARAMS> { params ->
                val response: RESPONSE = this.callback(params)
                call.respond(response)
            }
        }
    }
    return builtRoute
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.handleBody(
    method: HttpMethod,
    noinline callback: suspend PipelineContext<Unit, ApplicationCall>.(params: PARAMS, body: BODY) -> RESPONSE
): Route {
    lateinit var builtRoute: Route
    resource<PARAMS> {
        builtRoute = method(method) {
            rHandle<PARAMS> { params ->

                val parsedBody = try {
                    call.receive<BODY>()
                } catch (e: ContentTransformationException) {
                    serverError(HttpStatusCode.BadRequest, e.message ?: "body could not be parsed")
                }
                val response: RESPONSE = this.callback(params, parsedBody)
                call.respond(response)
            }
        }
    }
    return builtRoute
}

inline fun <reified PARAMS : Any, reified RESPONSE : Any> Route.get(noinline callback: suspend PipelineContext<Unit, ApplicationCall>.(params: PARAMS) -> RESPONSE) {
    handleNoBody(HttpMethod.Get, callback)
}


inline fun <reified PARAMS : Any, reified RESPONSE : Any> Route.head(noinline callback: suspend PipelineContext<Unit, ApplicationCall>.(params: PARAMS) -> RESPONSE) {
    handleNoBody(HttpMethod.Head, callback)
}


inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.post(noinline callback: suspend PipelineContext<Unit, ApplicationCall>.(params: PARAMS, body: BODY) -> RESPONSE) {
    handleBody(HttpMethod.Post, callback)
}


inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.put(noinline callback: suspend PipelineContext<Unit, ApplicationCall>.(params: PARAMS, body: BODY) -> RESPONSE) {
    handleBody(HttpMethod.Put, callback)
}


inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.delete(noinline callback: suspend PipelineContext<Unit, ApplicationCall>.(params: PARAMS, body: BODY) -> RESPONSE) {
    handleBody(HttpMethod.Delete, callback)
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.options(noinline callback: suspend PipelineContext<Unit, ApplicationCall>.(params: PARAMS, body: BODY) -> RESPONSE) {
    handleBody(HttpMethod.Options, callback)
}


inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.patch(noinline callback: suspend PipelineContext<Unit, ApplicationCall>.(params: PARAMS, body: BODY) -> RESPONSE) {
    handleBody(HttpMethod.Patch, callback)
}

