package io.thoth.openapi.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.dataconversion.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import io.ktor.util.converters.DataConversion as DataConversionImpl


inline fun <reified PARAMS : Any, reified RESPONSE : Any> Route.handleNoBody(
    method: HttpMethod, noinline callback: suspend PipelineContext<Unit, ApplicationCall>.(params: PARAMS) -> RESPONSE
) {
    val paramConverter: DataConversionImpl = application.pluginRegistry[DataConversion.key]

    method(method) {
        handle {
            val parsedParams = parseParametersIntoClass(paramConverter, call, PARAMS::class)
            val response = this.callback(parsedParams)
            call.respond(response)
        }
    }
}

inline fun <reified PARAMS : Any, reified BODY : Any, reified RESPONSE : Any> Route.handleBody(
    method: HttpMethod,
    noinline callback: suspend PipelineContext<Unit, ApplicationCall>.(params: PARAMS, body: BODY) -> RESPONSE
) {
    assert(method !== HttpMethod.Get && method !== HttpMethod.Head)

    val paramConverter: DataConversionImpl = application.pluginRegistry[DataConversion.key]

    method(method) {
        handle {

            val body = try {
                call.receive<BODY>()
            } catch (e: Exception) {
                TODO("Throw error")
            }

            val parsedParams = parseParametersIntoClass(paramConverter, call, PARAMS::class)
            val response = this.callback(parsedParams, body)
            call.respond(response)
        }
    }
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

