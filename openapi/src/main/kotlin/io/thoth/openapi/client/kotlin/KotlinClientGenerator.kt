package io.thoth.openapi.client.kotlin

import io.ktor.server.application.*
import io.thoth.openapi.client.common.ClientGenerator
import io.thoth.openapi.client.common.ClientPart
import io.thoth.openapi.client.common.TypeGenerator
import io.thoth.openapi.client.common.mappedKtReference
import io.thoth.openapi.common.getResourceContent
import io.thoth.openapi.ktor.OpenApiRoute
import io.thoth.openapi.ktor.plugins.OpenAPIConfigurationKey
import java.io.File
import java.nio.file.Path
import mu.KotlinLogging.logger

class KotlinClientGenerator(
    // TODO  UserPermissionsModel (to many imports)
    override val routes: List<OpenApiRoute>,
    private val packageName: String,
    private val apiClientName: String,
    dist: Path,
    fileWriter: ((File, String) -> Unit)?,
    typePackages: List<String>
) : ClientGenerator(dist, fileWriter) {
    private val log = logger {}

    private val requestRunner: String by lazy { getResourceContent("/RequestRunner.kt") }
    private val clientFunctions = mutableListOf<String>()
    private val clientImports = mutableSetOf<String>()
    private val typeDefinitions = mutableMapOf<String, KtGenerator.ReferenceType>()
    private val typeProvider =
        TypeGenerator.Provider(
            KtGenerator::class,
            listOf("io.thoth.openapi.client.kotlin") + typePackages,
        )

    init {
        initializeValues()
    }

    private fun getParameters(route: OpenApiRoute): String = buildString {
        // Path parameters
        (route.queryParameters + route.pathParameters).forEach { (param) ->
            val (actual, all) = typeProvider.generateTypes(param.type)
            clientImports += actual.imports
            typeDefinitions.putAll(all.mappedKtReference())
            append("${param.name}: ${actual.reference()}${if (param.optional) "?" else ""}, ")
        }

        // Body
        if (route.requestBodyType.clazz != Unit::class) {
            val (actual, all) = typeProvider.generateTypes(route.requestBodyType)
            clientImports += actual.imports
            typeDefinitions.putAll(all.mappedKtReference())
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

            val (responseBody, all) = typeProvider.generateTypes(route.responseBodyType)
            clientImports += responseBody.imports
            typeDefinitions.putAll(all.mappedKtReference())
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
                            if (it.imports.isNotEmpty()) {
                                append(it.imports.joinToString("\n"))
                                append("\n\n")
                            }
                            append(it.content())
                        },
                )
            }
        return parts
    }
}

fun Application.generateKotlinClient(
    packageName: String,
    apiClientName: String,
    dist: Path,
    routes: List<OpenApiRoute>? = null,
    fileWriter: ((File, String) -> Unit)? = null,
    typePackages: List<String> = emptyList()
) {
    KotlinClientGenerator(
            routes = routes ?: this.attributes[OpenAPIConfigurationKey].routeCollector.values(),
            packageName = packageName,
            dist = dist,
            apiClientName = apiClientName,
            fileWriter = fileWriter,
            typePackages = typePackages,
        )
        .safeClient()
}

fun Application.generateKotlinClient(
    packageName: String,
    apiClientName: String,
    dist: String,
    routes: List<OpenApiRoute>? = null,
    fileWriter: ((File, String) -> Unit)? = null,
    typePackages: List<String> = emptyList()
) =
    generateKotlinClient(
        packageName = packageName,
        dist = Path.of(dist),
        routes = routes,
        apiClientName = apiClientName,
        fileWriter = fileWriter,
        typePackages = typePackages,
    )
