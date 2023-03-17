package io.thoth.openapi.schema

import io.thoth.openapi.optional
import io.thoth.openapi.properties
import kotlin.reflect.KClass

data class QueryParameter(val name: String, val type: KClass<*>, val origin: KClass<*>, val optional: Boolean)

object QueryParameters {
    fun extractAll(resource: KClass<*>): List<QueryParameter> {
        return internalExtractAll(resource).values.toList()
    }

    private fun internalExtractAll(
        resource: KClass<*>,
        result: MutableMap<String, QueryParameter> = mutableMapOf()
    ): MutableMap<String, QueryParameter> {

        val pathParams = PathParameters.extractForClass(resource).map { it.name }.toSet()
        val queryParams =
            resource.properties
                .filter {
                    // Remove path parameters
                    it.name !in pathParams
                }
                .filter {
                    // Remove injected parent
                    it.returnType.classifier != resource.parent
                }
                .map {
                    QueryParameter(
                        name = it.name,
                        type = it.returnType.classifier as KClass<*>,
                        origin = resource,
                        optional = it.optional,
                    )
                }

        for (param in queryParams) {
            if (param.name in result) {
                throw IllegalStateException(
                    "Class ${resource.qualifiedName} has a query parameter " +
                        "called ${param.name} which is also used in ${result[param.name]!!.origin.qualifiedName}. " +
                        "Do not used duplicate parameters",
                )
            }
        }
        result.putAll(queryParams.associateBy { it.name })

        resource.parent?.run { internalExtractAll(this, result) }
        return result
    }
}
