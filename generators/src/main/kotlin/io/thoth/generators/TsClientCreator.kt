package io.thoth.generators

import io.thoth.generators.types.TsGenerator
import io.thoth.generators.types.generateTypes
import io.thoth.openapi.OpenApiRoute
import io.thoth.openapi.Summary
import java.io.File
import kotlin.reflect.full.findAnnotations

class TsClientCreator(
    private val routes: List<OpenApiRoute>,
    private val typesFile: File,
    private val clientFile: File,
) {
    private val typeDefinitions = mutableMapOf<String, TsGenerator.Type>()
    private val clientFunctions = mutableListOf<String>()
    // TODO: optional query types
    // TODO optional path types
    // TODO optional properties in interfaces
    // TODO interface inheritance
    // TODO correct api response type
    // TODO ignore private parameters in route definitions
    // TODO extract types from route definitions

    init {
        generateApiClient()
    }

    private companion object {

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
        };
    """
                .trimIndent() + "\n"
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
        };
        """
                .trimIndent() + "\n"
        }
    }

    private fun getRouteName(route: OpenApiRoute): String? {
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

    private fun getParameters(route: OpenApiRoute): String {
        val pathParams =
            route.pathParameters.joinToString(", ") {
                val tsType = generateTypes(it.first.origin)
                typeDefinitions.putAll(tsType.associateBy { it.name })
                "${it.first.name}: ${tsType.last().reference()}"
            }
        val queryParams =
            route.queryParameters.joinToString(", ") {
                val tsType = generateTypes(it.first.origin)
                typeDefinitions.putAll(tsType.associateBy { it.name })
                "${it.first.name}: ${tsType.last().reference()}"
            }
        val bodyParam =
            if (route.requestBodyType.clazz != Unit::class) {
                val tsTypes = generateTypes(route.requestBodyType)
                typeDefinitions.putAll(tsTypes.associateBy { it.name })
                "body: ${tsTypes.last().reference()}"
            } else ""
        return listOf(pathParams, queryParams, bodyParam).filter { it.isNotBlank() }.joinToString(", ")
    }

    private fun createURL(route: OpenApiRoute): String {
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

    private fun generateApiClient() {
        routes.forEach {
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

            val responseInterfaces = generateTypes(it.responseBodyType)
            typeDefinitions.putAll(responseInterfaces.associateBy { it.name })
        }
    }

    private fun createTypeImports(): String {
        return "import {${
            typeDefinitions.values.filter { !it.inline }.joinToString(", ") { it.reference() } + ", ApiResponse"
        }} from \"${this.typesFile.nameWithoutExtension}\";\n"
    }

    fun getClientFunctions(): String {
        return createTypeImports() +
            createUrlCreator() +
            createRequestMaker() +
            "export const api = {" +
            clientFunctions.joinToString(
                ",\n",
            ) {
                "  $it"
            } +
            "} as const;"
    }

    fun getClientTypes(): String {
        val internalTypes =
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
                .trimIndent() + "\n"
        return internalTypes +
            typeDefinitions.values.filter { !it.inline }.joinToString("\n\n") { "export ${it.content}" }
    }

    fun saveTypes() {
        typesFile.writeText(getClientTypes())
    }

    fun saveClient() {
        clientFile.writeText(getClientFunctions())
    }
}
