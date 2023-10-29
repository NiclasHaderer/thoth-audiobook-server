package io.thoth.openapi.client.kotlin

import io.ktor.client.request.*
import io.thoth.openapi.client.common.ClientGenerator
import io.thoth.openapi.client.common.ClientPart
import io.thoth.openapi.ktor.OpenApiRoute
import io.thoth.openapi.ktor.OpenApiRouteCollector
import java.nio.file.Path
import java.util.regex.Pattern
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder

class KotlinClientGenerator(
    override val routes: List<OpenApiRoute>,
    private val packageName: String,
    private val dist: Path,
) : ClientGenerator() {

    private val reflections = Reflections(
        ConfigurationBuilder().forPackages("io.thoth.openapi.client.kotlin").addScanners(Scanners.Resources),
    )

    private val requestRunner: String by lazy {
        object {}.javaClass.getResourceAsStream("/RequestRunner.kt")?.bufferedReader()?.readText()
            ?: error("Could not load RequestRunner.kt")
    }

    override fun generateClient(): List<ClientPart> {
        val parts = mutableListOf<ClientPart>()
        parts += ClientPart(path = dist.resolve("RequestRunner.kt"), content = requestRunner)
        return parts
    }
}

fun generateKotlinClient(
    packageName: String,
    dist: Path,
) {
    KotlinClientGenerator(
        routes = OpenApiRouteCollector.values(),
        packageName = packageName,
        dist = dist,
    ).safeClient()
}
