package io.thoth.openapi.client.kotlin

import io.thoth.openapi.client.common.ClientGenerator
import io.thoth.openapi.client.common.ClientPart
import io.thoth.openapi.ktor.OpenApiRoute
import io.thoth.openapi.ktor.OpenApiRouteCollector
import org.reflections.Reflections
import org.reflections.scanners.ResourcesScanner
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder
import java.nio.file.Path
import java.util.regex.Pattern

class KotlinClientGenerator(
    override val routes: List<OpenApiRoute>,
    private val packageName: String,
    private val dist: Path,
) : ClientGenerator() {

    private val reflections = Reflections(
        ConfigurationBuilder().forPackages("io.thoth.openapi.client.kotlin").addScanners(Scanners.Resources),
    )

    private val requestRunner: String by lazy {
        this::class.java.getResourceAsStream("/META-INF/resources/io/thoth/openapi/RequestRunner.kt")?.bufferedReader()
            ?.readText() ?: error("Cannot load RequestRunner.kt")
    }


    override fun generateClient(): List<ClientPart> {

        // ALl resources of the project
        val resources = reflections.getResources(Pattern.compile(".*\\.kt"))
        println(resources)


        val parts = mutableListOf<ClientPart>()
        parts += ClientPart(path = dist.resolve("client/kotlin/RequestRunner.kt"), content = requestRunner)

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
