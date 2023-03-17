package io.thoth.openapi.routing

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.resources.handle as resourceHandle
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import io.thoth.openapi.ErrorResponse
import io.thoth.openapi.SchemaHolder
import io.thoth.openapi.Secured
import io.thoth.openapi.findAnnotationUp
import io.thoth.openapi.properties
import io.thoth.openapi.responses.BaseResponse
import io.thoth.openapi.schema.parent
import kotlin.reflect.KClass
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

            throw ErrorResponse(
                HttpStatusCode.BadRequest,
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

fun assertParamsHierarchy(params: KClass<*>) {
    if (params::class == Unit::class) return
    // There are three conditions under which the hierarchy is valid:

    // 1. Every class over params is decorated as @Resource
    params.findAnnotation<Resource>()
        ?: throw IllegalStateException("Class ${params.qualifiedName} is not decorated as a resource")

    if (params.parent != null) {
        val parentClass = params.parent!!.java.kotlin
        // 2. Every class has a field which references the parent class name, because otherwise the
        // ktor routing will not
        //    work as one would think
        val hasDeclaredParent =
            params.properties.map { it.returnType.classifier as KClass<*> }.any { it == parentClass }
        if (!hasDeclaredParent) {
            throw IllegalStateException(
                "Class ${params.qualifiedName} has no property of type ${parentClass.qualifiedName}." +
                    "You have to create an additional property with the parent class as type",
            )
        }
        // 3. Every parent fulfills requirements 1 and 2
        assertParamsHierarchy(parentClass)
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
    // Check if the params route hierarchy is OK
    assertParamsHierarchy(PARAMS::class)

    // Add the route to the openapi schema
    SchemaHolder.addRouteToApi<PARAMS, BODY, RESPONSE>(fullPath(PARAMS::class), method)

    // Check if the route should be secured by ktor
    val secured = PARAMS::class.findAnnotationUp<Secured>()
    if (secured != null) {
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
