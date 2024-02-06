package io.thoth.openapi.client.kotlin

import io.ktor.server.application.*
import io.thoth.openapi.client.common.ClientGenerator
import io.thoth.openapi.client.common.ClientPart
import io.thoth.openapi.client.common.TypeGenerator
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
    private val abstract: Boolean,
    dist: Path,
    fileWriter: ((File, String) -> Unit)?,
    cleanDistPackage: Boolean,
    typePackages: List<String>
) : ClientGenerator(dist, fileWriter, cleanDistPackage) {
    private val logger = logger {}
    private val staticFiles by lazy {
        listOf(
            "RequestRunner" to getResourceContent("/kotlin/RequestRunner.kt"),
            "RequestMetadata" to getResourceContent("/kotlin/RequestMetadata.kt"),
            "OpenApiHttpResponse" to getResourceContent("/kotlin/OpenApiHttpResponse.kt"),
            "SerializationHolder" to getResourceContent("/kotlin/SerializationHolder.kt"),
        )
    }
    private val clientImports = mutableSetOf<String>()
    private val typeDefinitions = mutableMapOf<String, KtTypeGenerator.KtReferenceType>()
    private val typeProviders =
        TypeGenerator.Provider(
            KtTypeGenerator::class,
            listOf("io.thoth.openapi.client.kotlin") + typePackages,
        )

    override fun generateClient(): List<ClientPart> = buildList {
        val clientFunctions =
            routes.map {
                KtClientFunction(
                    getRouteName = this@KotlinClientGenerator::getRouteName,
                    route = it,
                    clientImports = clientImports,
                    typeDefinitions = typeDefinitions,
                    typeProviders = typeProviders,
                )
            }
        add(
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
                        append("import io.ktor.util.reflect.*\n")
                        append("import $packageName.models.*\n")
                        append("\n\n")

                        // Class
                        append("@Suppress(\"unused\")\n")
                        if (abstract) {
                            append("abstract class ${apiClientName}(\n")
                        } else {
                            append("open class ${apiClientName}(\n")
                        }
                        append("    baseUrl: Url\n")
                        append(") : RequestRunner(baseUrl) {\n")
                        append("${clientFunctions.map { it.content }.joinToString("\n\n")}\n")
                        append("}")
                    },
            ),
        )
        addAll(
            staticFiles.map { (name, content) ->
                ClientPart(
                    path = "$name.kt",
                    content =
                        buildString {
                            append("package $packageName\n\n")
                            append(content)
                        },
                )
            },
        )
        addAll(
            typeDefinitions.values.map {
                ClientPart(
                    path = "models/${it.name()}.kt",
                    content =
                        buildString {
                            append("package $packageName.models\n\n")
                            if (it.imports().isNotEmpty()) {
                                append(it.imports().joinToString("\n"))
                                append("\n\n")
                            }
                            append(it.content())
                        },
                )
            },
        )
        assertTypeNames(typeDefinitions)
    }

    private fun assertTypeNames(typeDefinitions: MutableMap<String, KtTypeGenerator.KtReferenceType>) {
        for (type in typeDefinitions.values) {
            // We only care about types that end with Impl
            if (!type.name().endsWith("Impl")) continue
            val nameWithSuffix = type.name()
            val nameWithoutSuffix = nameWithSuffix.removeSuffix("Impl")
            val typeWithoutSuffix = typeDefinitions[nameWithoutSuffix]
            if (typeWithoutSuffix != null) {
                logger.error {
                    buildString {
                        append("Type $nameWithSuffix exists as Impl and non-Impl type. ")
                        append("Only return the non-Impl type if the properties are the same. ")
                        append("If the properties are different, rename one of the types to avoid name clashes.")
                    }
                }
            }
        }
    }
}

fun Application.generateKotlinClient(
    packageName: String,
    apiClientName: String,
    dist: Path,
    routes: List<OpenApiRoute>? = null,
    fileWriter: ((File, String) -> Unit)? = null,
    typePackages: List<String> = emptyList(),
    abstract: Boolean = false,
    cleanDistPackage: Boolean = true
) {
    KotlinClientGenerator(
            routes = routes ?: this.attributes[OpenAPIConfigurationKey].routeCollector.values(),
            packageName = packageName,
            dist = dist,
            apiClientName = apiClientName,
            fileWriter = fileWriter,
            typePackages = typePackages,
            abstract = abstract,
            cleanDistPackage = cleanDistPackage,
        )
        .safeClient()
}

fun Application.generateKotlinClient(
    packageName: String,
    apiClientName: String,
    dist: String,
    routes: List<OpenApiRoute>? = null,
    fileWriter: ((File, String) -> Unit)? = null,
    typePackages: List<String> = emptyList(),
    abstract: Boolean = false,
    cleanDistPackage: Boolean = true
) =
    generateKotlinClient(
        packageName = packageName,
        dist = Path.of(dist),
        routes = routes,
        apiClientName = apiClientName,
        fileWriter = fileWriter,
        typePackages = typePackages,
        abstract = abstract,
        cleanDistPackage = cleanDistPackage,
    )
