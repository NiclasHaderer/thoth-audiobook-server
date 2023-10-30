package io.thoth.openapi.client.kotlin

import io.thoth.openapi.client.common.ClientGenerator
import io.thoth.openapi.client.common.ClientPart
import io.thoth.openapi.client.kotlin.types.KtGenerator
import io.thoth.openapi.common.getResourceContent
import io.thoth.openapi.ktor.OpenApiRoute
import io.thoth.openapi.ktor.OpenApiRouteCollector
import java.nio.file.Path
import mu.KotlinLogging.logger

class KotlinClientGenerator(
    override val routes: List<OpenApiRoute>,
    private val packageName: String,
    dist: Path,
) : ClientGenerator(dist) {
    private val log = logger {}

    private val requestRunner: String by lazy { getResourceContent("/RequestRunner.kt") }
    private val clientFunctions = mutableListOf<String>()
    private val typeDefinitions = mutableMapOf<String, KtGenerator.Type>()

    init {
        asdf()
    }

    private fun getParameters(route: OpenApiRoute): String {
        // Url parameter
        val urlParams = route.queryParameters + route.pathParameters
        val paramsStr =
            urlParams.map { (param) ->
                val (actual, all) = KtGenerator.generateTypes(param.type)
                typeDefinitions.putAll(
                    all.filterIsInstance<KtGenerator.ReferenceType>().associateBy { it.reference() },
                )
                "${param.name}: ${actual.reference()}${if (param.optional) "?" else ""}"
            }.toMutableList()

        // TODO Body parameter

        // TODO Request builder

        paramsStr += listOf("onBeforeRequest: OnBeforeRequest<*, *>")

        return paramsStr.joinToString(", ")
    }

    private fun asdf() {
        routes.forEach { route ->
            val routeName = getRouteName(route)
            if (routeName == null) {
                log.warn("Route ${route.method}:${route.fullPath} has no summary")
                return@forEach
            }

            val responseBody = KtGenerator.generateTypes(route.responseBodyType).first
            val function =
                """
                fun ${routeName}(${getParameters(route)}): ${responseBody.reference()} {
                    TODO("Not implemented")
                }
                """
                    .trimIndent()
            clientFunctions += function

            val responseTypes =
                KtGenerator.generateTypes(route.requestBodyType).second.filterIsInstance<KtGenerator.ReferenceType>()
            typeDefinitions.putAll(responseTypes.associateBy { it.reference() })
        }
    }

    override fun generateClient(): List<ClientPart> {
        val parts = mutableListOf<ClientPart>()
        parts += ClientPart(path = "RequestRunner.kt", content = requestRunner)
        parts += ClientPart(path = "Client.kt", content = clientFunctions.joinToString("\n\n"))
        parts += typeDefinitions.values.map { ClientPart(path = "types/${it.reference()}.kt", content = it.content()) }
        return parts
    }
}

fun generateKotlinClient(
    packageName: String,
    dist: Path,
    routes: List<OpenApiRoute> = OpenApiRouteCollector.values(),
) {
    KotlinClientGenerator(
            routes = routes,
            packageName = packageName,
            dist = dist,
        )
        .safeClient()
}

fun generateKotlinClient(
    packageName: String,
    dist: String,
    routes: List<OpenApiRoute> = OpenApiRouteCollector.values()
) = generateKotlinClient(packageName, Path.of(dist), routes)
