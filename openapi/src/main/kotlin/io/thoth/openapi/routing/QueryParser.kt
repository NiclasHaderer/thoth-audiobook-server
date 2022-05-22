package io.thoth.openapi.routing

import io.ktor.server.application.*
import io.ktor.util.converters.*
import io.ktor.util.reflect.*
import io.thoth.openapi.PathParam
import io.thoth.openapi.QueryParam
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.javaType


@OptIn(ExperimentalStdlibApi::class)
fun <T : Any> parseParametersIntoClass(
    converter: DataConversion,
    context: ApplicationCall,
    kClass: KClass<T>,
): T {
    // TODO better errors
    val (queryParams, pathParams) = getQueryAndPathParams(context)

    val constructor = kClass.primaryConstructor ?: throw Error("There has to be a primary constructor")

    val constructorCallArgs = mutableMapOf<KParameter, Any?>()


    for (parameter in constructor.parameters) {

        // Find path or query annotations for the constructor argument
        val pathAnnotation = parameter.findAnnotations<PathParam>().firstOrNull()
        val queryAnnotation = parameter.findAnnotations<QueryParam>().firstOrNull()

        // Every constructor parameter that is not optional has to be decorated as either a path or query parameter
        if (pathAnnotation == null && queryAnnotation == null && !parameter.isOptional) {
            throw Error("Parameter ${parameter.name} is not optional, but has not been decorated as a path, or a query param")
        }

        // Missing value for path parameter
        if (pathAnnotation != null && (parameter.name !in pathParams && !parameter.isOptional)) {
            throw Error("Path parameter ${parameter.name} is not optional, but you did not provide a value")
        }
        // Missing value for query parameter
        else if (queryAnnotation != null && (parameter.name !in queryParams && !parameter.isOptional)) {
            throw Error("Query parameter ${parameter.name} is not optional, but you did not provide a value")
        }

        // Should the value of the constructor query parameter be read from the query params or the path params
        val parameterSource = if (pathAnnotation != null) {
            pathParams
        } else {
            if (queryAnnotation != null) queryParams else continue
        }

        // Try to parse the url or query parameter in the constructorCallArgs map, which will get passed on to the
        // primary constructor
        try {
            val parameterTypeInfo =
                TypeInfo(parameter.type.classifier as KClass<*>, parameter.type.javaType, parameter.type)
            val requestParameterValue = converter.fromValues(parameterSource[parameter.name]!!, parameterTypeInfo)
            constructorCallArgs[parameter] = requestParameterValue
        } catch (e: DataConversionException) {
            throw Error("Error in conversion of ${parameter.name}: ${e.message}")
        }
    }

    return constructor.callBy(constructorCallArgs)
}


data class QueryPathParams(
    val queryParams: MutableMap<String, List<String>>,
    val pathParams: MutableMap<String, List<String>>
)

fun getQueryAndPathParams(call: ApplicationCall): QueryPathParams {
    val queryParamKeys = call.request.queryParameters.entries().map { it.key }
    val pathParamsKeys = call.parameters.entries().map { it.key }.filter { it !in queryParamKeys }

    val queryParams = mutableMapOf<String, List<String>>()
    for (key in queryParamKeys) {
        queryParams[key] = call.request.queryParameters.getAll(key)!!
    }

    val pathParams = mutableMapOf<String, List<String>>()
    for (key in pathParamsKeys) {
        pathParams[key] = call.parameters.getAll(key)!!
    }

    return QueryPathParams(queryParams = queryParams, pathParams = pathParams)
}
