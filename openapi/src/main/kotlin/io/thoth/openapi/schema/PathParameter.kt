package io.thoth.openapi.schema

import io.ktor.resources.*
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

class PathParameter(val name: String, val type: KClass<*>, val origin: KClass<*>)

fun getPathParameters(
    params: KClass<*>,
    takenParameters: MutableMap<String, PathParameter> = mutableMapOf()
): List<PathParameter> {
    val resourcePath = params.findAnnotation<Resource>()!!.path
    val matches = "\\{((?:[a-z]|[A-Z]|_)+)}".toRegex().findAll(resourcePath)
    for (match in matches) {
        val varName = match.groupValues[1]
        // Check if the variable name is already taken
        if (takenParameters.containsKey(varName)) {
            throw IllegalStateException(
                "Class ${params.qualifiedName} has a duplicate path parameter name $varName. " +
                    "The parameter is already taken by ${takenParameters[varName]!!.origin}",
            )
        }

        // Check if the variable name is a valid kotlin variable name
        val varMember =
            params.declaredMemberProperties.find { it.name == varName }
                ?: throw IllegalStateException(
                    "Class ${params.qualifiedName} has a path parameter $varName which is not declared as a member. " +
                        "You have to create a property with the name $varName",
                )

        takenParameters[varName] =
            PathParameter(name = varName, type = varMember.returnType.classifier as KClass<*>, origin = params)
    }

    // Go up and check the parent
    params.parent?.java?.kotlin?.run { getPathParameters(this, takenParameters) }
    return takenParameters.values.toList()
}
