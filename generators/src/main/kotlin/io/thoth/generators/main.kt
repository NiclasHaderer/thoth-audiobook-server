package io.thoth.generators

import io.thoth.openapi.OpenApiRoute
import io.thoth.openapi.OpenApiRouteCollector
import io.thoth.openapi.Summary
import java.io.File
import kotlin.collections.set
import kotlin.reflect.full.findAnnotations

fun getName(route: OpenApiRoute): String? {
    val summary =
        route.requestParamsType.clazz.findAnnotations<Summary>().firstOrNull { it.method == route.method.value }
    val summaryString = summary?.summary ?: return null
    return summaryString
        .split(" ")
        .mapIndexed { index, word ->
            word.replaceFirstChar {
                if (it.isLowerCase()) {
                    it.titlecase()
                } else if (index == 0) {
                    it.lowercase()
                } else {
                    it.toString()
                }
            }
        }
        .joinToString("")
}

fun getParameters(route: OpenApiRoute): String {
    val pathParams = route.pathParameters.joinToString(", ") { "${it.first.name}: ${"string"}" }
    val queryParams = route.queryParameters.joinToString(", ") { it.first.name }
    val bodyParam = if (route.requestBodyType.clazz != Unit::class) "body" else ""
    return listOf(pathParams, queryParams, bodyParam).filter { it.isNotBlank() }.joinToString(", ")
}

class TypescriptType(val name: String, val content: String)

fun generateTypes(route: OpenApiRoute, collectedTypes: MutableMap<String, TypescriptType>) {
    route.pathParameters.forEach {
        collectedTypes[it.first.name] = TypescriptType(it.first.name, it.first.type.toString())
    }
    route.queryParameters.forEach {
        collectedTypes[it.first.name] = TypescriptType(it.first.name, it.first.type.toString())
    }
    route.requestBodyType.let {
        collectedTypes[it.clazz.qualifiedName!!] = TypescriptType(it.clazz.qualifiedName!!, it.clazz.toString())
    }

    route.responseBodyType.let {
        collectedTypes[it.clazz.qualifiedName!!] = TypescriptType(it.clazz.qualifiedName!!, it.clazz.toString())
    }
}

fun createUrlCreator(): String {
    return """
        const __createUrl = (route: string, params: Record<string, string | number | boolean | undefined | null>): string => {
            const __urlParams = new URLSearchParams(params);
            [...__urlParams].forEach(([key, value]) => {
                if (value === "undefined" || value === "null" || value === "") {
                    __urlParams.delete(key)
                }
            });
            
            const __finalUrlParams = __urlParams.toString();
            return __finalUrlParams ? route + "?" + __finalUrlParams : route;
        }
    """
        .trimIndent()
}

fun createRequestMaker(): String {
    return """
        const __request = <T>(route: string, method: string, body?: object): Promise<{ success: true, body: T } | {
            success: false,
            error: string | object
        }> => {
            return fetch(route, {
                method,
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify(body)
            })
                .then(async (response) => {
                    if (response.ok) {
                        return {
                            success: true,
                            body: await response.json()
                        } as const
                    } else {
                        return {
                            success: false,
                            error: await response.json().catch(() => response.text()).catch(() => response.statusText)
                        } as const
                    }
                })
                .catch((error) => ({success: false, error: error?.toString()} as const))
        }
    """
        .trimIndent()
}

fun createURL(route: OpenApiRoute): String {
    // Add them to the URLSearchParams
    val finalPath = route.fullPath.replace("{", "\${")
    return """
        ${
        if (route.queryParameters.isEmpty()) {
            "const __finalUrl = `$finalPath`;"
        } else {
            "const __finalUrl = __createUrl(`$finalPath`, {${route.queryParameters.joinToString(", ") { it.first.name }}});"
        }
    }
    """
        .trimIndent()
}

fun generateTypescriptTypes() {
    val collectedTypes = mutableMapOf<String, TypescriptType>()
    val functions = mutableListOf<String>()
    OpenApiRouteCollector.forEach {
        val routeName = getName(it)
        if (routeName == null) {
            println("Route ${it.method}:${it.fullPath} has no summary")
            return@forEach
        }
        val parameters = getParameters(it)
        val function =
            """
            $routeName($parameters) {
                ${createURL(it)}
                return __request(__finalUrl, "${it.method.value}", ${if (it.requestBodyType.clazz != Unit::class) "body" else "undefined"});
            }
        """
                .trimIndent()
        functions.add(function)
        generateTypes(it, collectedTypes)
    }

    // Save to file
    val file = File("./api.ts")
    file.printWriter().use { out ->
        out.println(createUrlCreator())
        out.println(createRequestMaker())
        out.println(
            """
            export const api = {
                ${functions.joinToString(",\n")}
            } as const;
        """
                .trimIndent(),
        )
    }
}
