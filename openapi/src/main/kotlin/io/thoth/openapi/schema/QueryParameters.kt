package io.thoth.openapi.schema

import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField

class QueryParameter(val name: String, val type: KClass<*>, val origin: KClass<*>)

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
            resource.declaredMemberProperties
                .filter {
                    // checks if the property is a getter
                    it.javaField != null
                }
                .filter {
                    // Remove path parameters
                    it.name !in pathParams
                }
                .filter {
                    // Remove injected parent
                    it.returnType.classifier != resource.parent
                }
                .map { QueryParameter(it.name, it.returnType.classifier as KClass<*>, resource) }

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
