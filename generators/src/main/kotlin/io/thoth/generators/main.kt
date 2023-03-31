package io.thoth.generators

import io.thoth.openapi.OpenApiRoute
import io.thoth.openapi.OpenApiRouteCollector
import io.thoth.openapi.Summary
import java.io.File
import kotlin.reflect.full.findAnnotations

fun getRouteName(route: OpenApiRoute): String? {
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

fun generateTypes(): List<String> {
    return listOf()
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
        const __request = <T>(route: string, method: string, body?: object): Promise<ApiResponse<T>> => {
            return fetch(route, {
                method,
                headers: {
                    "Content-Type": "application/json",
                },
                body: body ? JSON.stringify(body) : undefined
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
                            status: response.status,
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

fun generateApiClient(): List<String> {
    val clientFunctions = mutableListOf<String>()
    OpenApiRouteCollector.forEach {
        val routeName = getRouteName(it)
        if (routeName == null) {
            println("Route ${it.method}:${it.fullPath} has no summary")
            return@forEach
        }
        val function =
            """
            $routeName(${getParameters(it)}) {
                ${createURL(it)}
                return __request(__finalUrl, "${it.method.value}", ${if (it.requestBodyType.clazz != Unit::class) "body" else "undefined"});
            }
        """
                .trimIndent()
        clientFunctions.add(function)
    }
    return clientFunctions
}

fun saveClientFunctionsToFile(functions: List<String>, file: File) {
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

fun saveTypesToFile(types: List<String>, typesFile: File) {
    typesFile.printWriter().use { out ->
        out.println(
            """
            export type ApiError = {
                success: false,
                error: string | object
                status?: number
            }
            
            export type ApiSuccess<T> = {
                success: true,
                body: T
            }
            
            export type ApiResponse<T> = ApiError | ApiSuccess<T>
        """
                .trimIndent(),
        )
        out.println(types.joinToString("\n"))
    }
}

fun File.prependText(content: String) {
    val tempFile = File(this.absolutePath + ".tmp")
    tempFile.writeText(content)
    tempFile.appendBytes(this.readBytes())
    this.delete()
    tempFile.renameTo(this)
}

fun addTypeImports(types: List<String>, file: File, typesFile: File) {
    file.prependText(
        """
        import { ApiResponse } from "./types";

    """
            .trimIndent(),
    )
    // TODO add imports for types
    file.prependText(
        """
        """
            .trimIndent(),
    )
}

fun generateTypescriptTypes() {
    val functions = generateApiClient()
    val apiFile = File("./api.ts")
    val typesFile = File("./types.ts")
    saveClientFunctionsToFile(functions, apiFile)

    val types = generateTypes()
    addTypeImports(types, apiFile, typesFile)
    saveTypesToFile(types, typesFile)
}
