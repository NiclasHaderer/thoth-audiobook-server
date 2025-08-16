package io.thoth.openapi.client.kotlin

import io.ktor.server.application.*
import io.thoth.openapi.client.common.ClientGenerator
import io.thoth.openapi.client.common.ClientPart
import io.thoth.openapi.client.common.TypeGenerator
import io.thoth.openapi.common.getResourceContent
import io.thoth.openapi.ktor.OpenApiRoute
import io.thoth.openapi.ktor.plugins.OpenAPIConfigurationKey
import mu.KotlinLogging.logger
import java.io.File
import java.nio.file.Path

enum class KtErrorHandling {
    Result,
    Exception,
    Either,
}

class KotlinClientGenerator(
    // TODO  UserPermissionsModel (to many imports)
    override val routes: List<OpenApiRoute>,
    private val packageName: String,
    private val apiClientName: String,
    private val abstract: Boolean,
    private val errorHandling: KtErrorHandling,
    dist: Path,
    fileWriter: ((File, String) -> Unit)?,
    cleanDistPackage: Boolean,
    directoryToScanForTypes: List<String>,
) : ClientGenerator(dist, fileWriter, cleanDistPackage) {
    private val logger = logger {}
    private val staticFiles by lazy {
        listOf(
            "RequestRunner" to getResourceContent("/kotlin/RequestRunner.kt"),
            "ApiError" to getResourceContent("/kotlin/ApiError.kt"),
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
            listOf("io.thoth.openapi.client.kotlin") + directoryToScanForTypes,
        )

    override fun generateClient(): List<ClientPart> =
        buildList {
            val clientFunctions =
                routes.map {
                    KtClientFunction(
                        getRouteName = this@KotlinClientGenerator::getRouteName,
                        route = it,
                        clientImports = clientImports,
                        typeDefinitions = typeDefinitions,
                        typeProviders = typeProviders,
                        errorHandling = errorHandling,
                    )
                }

            val base =
                buildString {
                    append("@file:Suppress(\"UnusedImport\")\n\n")
                    // Package
                    append("package $packageName\n\n")

                    // Imports
                    append("import io.ktor.client.*\n")
                    append("import io.ktor.http.*\n")
                    append("import io.ktor.client.call.*\n")
                    append("import io.ktor.client.plugins.*\n")
                    append("import io.ktor.util.reflect.*\n")

                    // Client imports
                    append(clientImports.joinToString("\n"))
                    append("\n")

                    // Optional error handling imports
                    if (errorHandling == KtErrorHandling.Either) {
                        append("import arrow.core.Either\n")
                        append("import java.io.IOException\n")
                    }

                    // Model imports
                    append("import $packageName.models.*\n")
                    append("\n\n")
                }

            add(
                ClientPart(
                    path = "$apiClientName.kt",
                    content =
                        buildString {
                            append(base)
                            // Class
                            append("interface $apiClientName {\n")
                            append("${clientFunctions.map { it.content }.joinToString("\n\n")}\n")
                            append("}")
                        },
                ),
            )
            add(
                ClientPart(
                    path = "${apiClientName}Impl.kt",
                    content =
                        buildString {
                            append(base)

                            // Class
                            append("@Suppress(\"unused\")\n")
                            if (abstract) {
                                append("abstract class ${apiClientName}Impl(\n")
                            } else {
                                append("open class ${apiClientName}Impl(\n")
                            }
                            append("    baseUrl: Url\n")
                            append(") : RequestRunner(baseUrl), $apiClientName {\n")
                            append("${clientFunctions.map { it.contentImpl }.joinToString("\n\n")}\n")
                            if (errorHandling == KtErrorHandling.Either) {
                                val eitherFun = getResourceContent("/kotlin/Either.kt")
                                // Replace everything up to // -- Start and after // -- End
                                append(eitherFun.substringAfter("// -- Start").substringBefore("// -- End"))
                            }
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
    apiClientPackageName: String,
    apiClientName: String,
    savePath: Path,
    routes: List<OpenApiRoute>? = null,
    fileWriter: ((File, String) -> Unit)? = null,
    directoryToScanForTypes: List<String> = emptyList(),
    abstract: Boolean = true,
    cleanDistPackage: Boolean = true,
    errorHandling: KtErrorHandling = KtErrorHandling.Either,
) {
    KotlinClientGenerator(
        routes = routes ?: this.attributes[OpenAPIConfigurationKey].routeCollector.values(),
        packageName = apiClientPackageName,
        dist = savePath,
        apiClientName = apiClientName,
        fileWriter = fileWriter,
        directoryToScanForTypes = directoryToScanForTypes,
        abstract = abstract,
        cleanDistPackage = cleanDistPackage,
        errorHandling = errorHandling,
    ).safeClient()
}

fun Application.generateKotlinClient(
    apiClientPackageName: String,
    apiClientName: String,
    savePath: String,
    routes: List<OpenApiRoute>? = null,
    fileWriter: ((File, String) -> Unit)? = null,
    directoryToScanForTypes: List<String> = emptyList(),
    abstract: Boolean = true,
    cleanDistPackage: Boolean = true,
    errorHandling: KtErrorHandling = KtErrorHandling.Either,
) = generateKotlinClient(
    apiClientPackageName = apiClientPackageName,
    savePath = Path.of(savePath),
    routes = routes,
    apiClientName = apiClientName,
    fileWriter = fileWriter,
    directoryToScanForTypes = directoryToScanForTypes,
    abstract = abstract,
    cleanDistPackage = cleanDistPackage,
    errorHandling = errorHandling,
)
