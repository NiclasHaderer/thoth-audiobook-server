package io.thoth.openapi.client.kotlin

import io.thoth.openapi.client.common.ClientGenerator
import io.thoth.openapi.client.common.ClientPart
import io.thoth.openapi.ktor.OpenApiRoute
import io.thoth.openapi.ktor.OpenApiRouteCollector
import java.nio.file.Path
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder

class KotlinClientGenerator(
    override val routes: List<OpenApiRoute>,
    private val packageName: String,
    dist: Path,
) : ClientGenerator(dist) {

    private val reflections =
        Reflections(
            ConfigurationBuilder().forPackages("io.thoth.openapi.client.kotlin").addScanners(Scanners.Resources),
        )

    private val requestRunner: String by lazy {
        object {}.javaClass.getResourceAsStream("/RequestRunner.kt")?.bufferedReader()?.readText()
            ?: error("Could not load RequestRunner.kt")
    }

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
