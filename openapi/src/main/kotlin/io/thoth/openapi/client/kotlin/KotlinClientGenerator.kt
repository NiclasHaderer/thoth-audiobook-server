package io.thoth.openapi.client.kotlin

import io.thoth.openapi.client.common.ClientGenerator
import io.thoth.openapi.client.common.ClientPart
import io.thoth.openapi.common.getResourceContent
import io.thoth.openapi.ktor.OpenApiRoute
import io.thoth.openapi.ktor.OpenApiRouteCollector
import java.nio.file.Path
import mu.KotlinLogging.logger

class KotlinClientGenerator(
    override val routes: List<OpenApiRoute>,
    private val packageName: String,
    private val apiClientName: String,
    dist: Path,
) : ClientGenerator(dist) {
    private val log = logger {}

    private val requestRunner: String by lazy { getResourceContent("/RequestRunner.kt") }
    private val clientFunctions = mutableListOf<String>()
    private val typeDefinitions = mutableMapOf<String, KtGenerator.ReferenceType>()

    init {
        initializeValues()
    }

    private fun getParameters(route: OpenApiRoute): String {
        // Url parameter
        val urlParams = route.queryParameters + route.pathParameters
        val paramsStr =
            urlParams
                .map { (param) ->
                    val (actual, all) = KtGenerator.generateTypes(param.type)
                    typeDefinitions.putAll(
                        all.filterIsInstance<KtGenerator.ReferenceType>().associateBy { it.reference() },
                    )
                    "${param.name}: ${actual.reference()}${if (param.optional) "?" else ""}"
                }
                .toMutableList()

        // Body
        if (route.requestBodyType.clazz != Unit::class) {
            val (actual, all) = KtGenerator.generateTypes(route.requestBodyType)
            typeDefinitions.putAll(all.filterIsInstance<KtGenerator.ReferenceType>().associateBy { it.reference() })
            paramsStr += listOf("body: ${actual.reference()}")
        }

        // Headers
        paramsStr += listOf("headers: Headers = Headers.Empty")

        // Hooks to modify the request
        paramsStr += listOf("onBeforeRequest: OnBeforeRequest<*, *> = { _, _ -> }")
        paramsStr += listOf("onAfterRequest: OnAfterRequest<*, *> = { _, _ -> }")

        return paramsStr.joinToString(", ")
    }

    private fun initializeValues() {
        routes.forEach { route ->
            val routeName = getRouteName(route)
            if (routeName == null) {
                log.warn("Route ${route.method}:${route.fullPath} has no summary")
                return@forEach
            }

            val (responseBody, all) = KtGenerator.generateTypes(route.responseBodyType)
            typeDefinitions.putAll(all.filterIsInstance<KtGenerator.ReferenceType>().associateBy { it.reference() })
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
        parts += ClientPart(path = "RequestRunner.kt", content = requestRunner)
        parts +=
            ClientPart(
                path = "${apiClientName}.kt",
                content =
                    run {
                        clientFunctions.joinToString("\n\n")

                        "open class ${apiClientName}(\n" +
                            "    clientBuilder: HttpClientConfig<*>.() -> Unit = {}\n" +
                            ") : RequestRunner(clientBuilder) {\n" +
                            "${clientFunctions.joinToString("\n\n")}\n" +
                            "}"
                    },
            )
        parts +=
            typeDefinitions.values.map {
                ClientPart(
                    path = "types/${it.name()}.kt",
                    content =
                        run {
                            val imports = it.imports.joinToString("\n")
                            val content = it.content()
                            "package $packageName.types\n\n" + "$imports\n\n" + content
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
) {
    KotlinClientGenerator(
            routes = routes,
            packageName = packageName,
            dist = dist,
            apiClientName = apiClientName,
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
