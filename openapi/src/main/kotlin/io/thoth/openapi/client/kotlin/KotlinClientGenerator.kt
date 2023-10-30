package io.thoth.openapi.client.kotlin

import io.thoth.openapi.client.common.ClientGenerator
import io.thoth.openapi.client.common.ClientPart
import io.thoth.openapi.common.getResourceContent
import io.thoth.openapi.ktor.OpenApiRoute
import io.thoth.openapi.ktor.OpenApiRouteCollector
import java.nio.file.Path

class KotlinClientGenerator(
    override val routes: List<OpenApiRoute>,
    private val packageName: String,
    dist: Path,
) : ClientGenerator(dist) {

    private val requestRunner: String by lazy { getResourceContent("/RequestRunner.kt") }

    override fun generateClient(): List<ClientPart> {
        val parts = mutableListOf<ClientPart>()
        parts += ClientPart(path = "RequestRunner.kt", content = requestRunner)
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
