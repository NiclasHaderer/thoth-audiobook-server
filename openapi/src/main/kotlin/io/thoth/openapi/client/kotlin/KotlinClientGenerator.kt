package io.thoth.openapi.client.kotlin

import io.thoth.openapi.client.common.ClientGenerator
import io.thoth.openapi.client.common.ClientPart
import io.thoth.openapi.common.getResourceContent
import io.thoth.openapi.ktor.OpenApiRoute
import io.thoth.openapi.ktor.OpenApiRouteCollector
import java.io.File
import java.nio.file.Path
import mu.KotlinLogging.logger

class KotlinClientGenerator(
    override val routes: List<OpenApiRoute>,
    private val packageName: String,
    private val apiClientName: String,
    dist: Path,
    fileWriter: ((File, String) -> Unit)?,
) : ClientGenerator(dist, fileWriter) {
    private val log = logger {}

    private val requestRunner: String by lazy { getResourceContent("/RequestRunner.kt") }
    private val clientFunctions = mutableListOf<String>()
    private val clientImports = mutableSetOf<String>()
    private val typeDefinitions = mutableMapOf<String, KtGenerator.ReferenceType>()

    init {
        // TODO imports for list, pair, maps, ...
        // TODO override val for interfaces (or omit)
        initializeValues()
    }

    private fun getParameters(route: OpenApiRoute): String = buildString {
        // Path parameters
        (route.queryParameters + route.pathParameters).forEach { (param) ->
            val (actual, all) = KtGenerator.generateTypes(param.type)
            clientImports += actual.imports
            typeDefinitions.putAll(all.mappedReference())
            append("${param.name}: ${actual.reference()}${if (param.optional) "?" else ""}, ")
        }

        // Body
        if (route.requestBodyType.clazz != Unit::class) {
            val (actual, all) = KtGenerator.generateTypes(route.requestBodyType)
            clientImports += actual.imports
            typeDefinitions.putAll(all.mappedReference())
            append("body: ${actual.reference()}, ")
        }

        // Headers
        append("headers: Headers = Headers.Empty, ")

        // Hooks to modify the request
        append("onBeforeRequest: OnBeforeRequest<*, *> = { _, _ -> }, ")
        append("onAfterRequest: OnAfterRequest<*, *> = { _, _ -> }")
    }

    private fun initializeValues() {
        routes.forEach { route ->
            val routeName = getRouteName(route)
            if (routeName == null) {
                log.warn("Route ${route.method}:${route.fullPath} has no summary")
                return@forEach
            }

            val (responseBody, all) = KtGenerator.generateTypes(route.responseBodyType)
            clientImports += responseBody.imports
            typeDefinitions.putAll(all.mappedReference())
            val function = buildString {
                append("    open fun ${routeName}(${getParameters(route)}): ${responseBody.reference()} {\n")
                append("        TODO(\"Not implemented\")")
                append("\n    }")
            }
            clientFunctions += function
        }
    }

    override fun generateClient(): List<ClientPart> {
        val parts = mutableListOf<ClientPart>()
        parts +=
            ClientPart(
                path = "RequestRunner.kt",
                content =
                    buildString {
                        append("package $packageName\n\n")
                        append(requestRunner)
                    },
            )
        parts +=
            ClientPart(
                path = "${apiClientName}.kt",
                content =
                    buildString {
                        // Package
                        append("package $packageName\n\n")

                        // Imports
                        append("import io.ktor.client.*\n")
                        append(clientImports.joinToString("\n"))
                        append("\n")
                        append("import io.ktor.http.*\n")
                        append("import $packageName.models.*\n")
                        append("\n\n")

                        // Class
                        append("open class ${apiClientName}(\n")
                        append("    clientBuilder: HttpClientConfig<*>.() -> Unit = {}\n")
                        append(") : RequestRunner(clientBuilder) {\n")
                        append("${clientFunctions.joinToString("\n\n")}\n")
                        append("}")
                    },
            )
        parts +=
            typeDefinitions.values.map {
                ClientPart(
                    path = "models/${it.name()}.kt",
                    content =
                        buildString {
                            append("package $packageName.models\n\n")
                            append(it.imports.joinToString("\n"))
                            append("\n\n")
                            append(it.content())
                        },
                )
            }
        return parts
    }
}

fun generateKotlinClient(
    packageName: String,
    apiClientName: String,
    dist: Path,
    routes: List<OpenApiRoute> = OpenApiRouteCollector.values(),
    fileWriter: ((File, String) -> Unit)? = null,
) {
    KotlinClientGenerator(
            routes = routes,
            packageName = packageName,
            dist = dist,
            apiClientName = apiClientName,
            fileWriter = fileWriter,
        )
        .safeClient()
}

fun generateKotlinClient(
    packageName: String,
    apiClientName: String,
    dist: String,
    routes: List<OpenApiRoute> = OpenApiRouteCollector.values()
) =
    generateKotlinClient(
        packageName = packageName,
        dist = Path.of(dist),
        routes = routes,
        apiClientName = apiClientName,
    )
