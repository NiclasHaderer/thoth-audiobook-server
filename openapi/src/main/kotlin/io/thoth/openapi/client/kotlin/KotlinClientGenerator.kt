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
    }
}

/*
@OptIn(InternalAPI::class)
fun cleanupTypes(typesMap: Map<String, KtTypeGenerator.KtReferenceType>) {
    //  Iterate over all type definitions and check the following:
    //  1. If the type ends with Impl AND there is a type with the same name but without Impl,
    //     then check if the type without Impl has the same properties as the Impl type.
    //     If so, then modify the Impl type, so it has the same name, thereby generating the same type.
    //  2. If the type ends with Impl AND there is a type with the same name but without Impl,
    //     but the type without Impl has different properties, then we have to keep both types.
    //     The name of the Impl type will be changed to Implementation to avoid name clashes.
    //  3. If the type ends with Impl AND there is no type with the same name but without Impl,
    //     then we can just remove the Impl from the types' name, as a type without Impl in the name will also
    //     generate an Impl type.
    for (type in typesMap.values) {
        // We only care about types that end with Impl
        if (!type.name().endsWith("Impl")) continue

        val nameWithSuffix = type.name()
        val nameWithoutSuffix = nameWithSuffix.removeSuffix("Impl")
        val typeWithoutSuffix = typesMap[nameWithoutSuffix]
        // Case 3
        if (typeWithoutSuffix == null) {
            // 1. Update name
            type.name = nameWithoutSuffix
            // 2. Update reference
            type.reference.replace(nameWithSuffix, nameWithoutSuffix)
            // 3. Update the content
            type.content.replace(nameWithSuffix, nameWithoutSuffix)
        } else {
            // Case 1

            val contentIsEqual = { withoutImpl: KtTypeGenerator.KtReferenceType, withImpl: KtTypeGenerator.KtReferenceType ->
                val withoutImplContent = withoutImpl.content()
                val withImplContent = withImpl.content().also {
                    it.replace(withImpl.name(), withoutImpl.name())
                }
                withoutImplContent == withImplContent
            }


            if (contentIsEqual(typeWithoutSuffix, type)) {
                // 1. Update name
                type.name = nameWithoutSuffix
                // 2. Update reference
                type.reference.replace(nameWithSuffix, nameWithoutSuffix)
                // 3. Update the content
                type.content.replace(nameWithSuffix, nameWithoutSuffix)
            } else {
                // Case 2
                // 1. Update name
                type.name = "${nameWithoutSuffix}Implementation"
                // 2. Update reference
                type.reference.replace(nameWithSuffix, "${nameWithoutSuffix}Implementation")
                // 3. Update the content
                type.content.replace(nameWithSuffix, "${nameWithoutSuffix}Implementation")
            }
        }
    }
}
*/

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
