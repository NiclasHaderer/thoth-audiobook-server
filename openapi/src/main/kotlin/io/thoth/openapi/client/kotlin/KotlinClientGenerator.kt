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
    private val requestRunner: String by lazy { getResourceContent("/RequestRunner.kt") }
    private val clientImports = mutableSetOf<String>()
    private val typeDefinitions = mutableMapOf<String, KtGenerator.ReferenceType>()
    private val typeProviders = TypeGenerator.Provider(
        KtGenerator::class,
        listOf("io.thoth.openapi.client.kotlin") + typePackages,
    )
    private val clientFunctions by lazy {
        routes.map {
            KtClientFunction(
                getRouteName = this::getRouteName,
                route = it,
                clientImports = clientImports,
                typeDefinitions = typeDefinitions,
                typeProviders = typeProviders,
            )
        }
    }

    override fun generateClient(): List<ClientPart> = buildList {
        add(
            ClientPart(
                path = "RequestRunner.kt",
                content = buildString {
                    append("package $packageName\n\n")
                    append(requestRunner)
                },
            ),
        )
        add(
            ClientPart(
                path = "${apiClientName}.kt",
                content = buildString {
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
                    append("${clientFunctions.map { it.generateContent() }.joinToString("\n\n")}\n")
                    append("}")
                },
            ),
        )
        addAll(
            typeDefinitions.values.map {
                ClientPart(
                    path = "models/${it.name()}.kt",
                    content = buildString {
                        append("package $packageName.models\n\n")
                        if (it.imports.isNotEmpty()) {
                            append(it.imports.joinToString("\n"))
                            append("\n\n")
                        }
                        append(it.content())
                    },
                )
            },
        )

        // TODO
        //  Iterate over all type definitions and check the following:
        //  1. If the type ends with Impl AND there is a type with the same name but without Impl,
        //     then check if the type without Impl has the same properties as the Impl type.
        //     If so, then just remove the Impl type, as the non-Impl type will generate an Impl type as well
        //  2. If the type ends with Impl AND there is a type with the same name but without Impl,
        //     but the type without Impl has different properties, then we have to keep both types.
        //  3. If the type ends with Impl AND there is no type with the same name but without Impl,
        //     then we can just remove the Impl from the types name, as a type without Impl in the name will also
        //     generate an Impl type.
        // TODO Now we have to make sure that every reference that has been made up to this point is still valid.
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
    ).safeClient()
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
) = generateKotlinClient(
    packageName = packageName,
    dist = Path.of(dist),
    routes = routes,
    apiClientName = apiClientName,
    fileWriter = fileWriter,
    typePackages = typePackages,
    abstract = abstract,
    cleanDistPackage = cleanDistPackage,
)
