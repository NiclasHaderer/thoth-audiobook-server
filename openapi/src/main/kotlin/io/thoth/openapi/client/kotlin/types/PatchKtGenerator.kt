package io.thoth.openapi.client.kotlin.types

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.kotlin.KtTypeGenerator
import io.thoth.openapi.common.ClassType
import io.thoth.openapi.common.Patch

// Clients use the Patch from openapi-models, the same class the server's PatchModule serializes
class PatchKtGenerator : KtTypeGenerator() {
    private val log = logger {}

    override fun generateContent(
        classType: ClassType,
        generateSubType: GenerateType<KtType>,
    ): String = generateContent(classType, generateSubType, impl = false)

    override fun generateImplContent(
        classType: ClassType,
        generateSubType: GenerateType<KtType>,
    ): String = generateContent(classType, generateSubType, impl = true)

    private fun generateContent(
        classType: ClassType,
        generateSubType: GenerateType<KtType>,
        impl: Boolean,
    ): String {
        val argument = classType.genericArguments.singleOrNull()
        if (argument == null) {
            log.warn { "Patch type without generic arguments" }
            return "Patch<*>"
        }
        val value = generateSubType(argument)
        val nullable = if (argument.isNullable) "?" else ""
        return "Patch<${if (impl) value.referenceImpl() else value.reference()}$nullable>"
    }

    override fun withImports(
        classType: ClassType,
        generateSubType: GenerateType<KtType>,
    ): List<String> =
        listOf("import ${Patch::class.qualifiedName}") +
            classType.genericArguments.flatMap { generateSubType(it).imports() }

    override fun getInsertionMode(classType: ClassType) = KtDataType.PRIMITIVE

    override fun getName(classType: ClassType): String = "Patch"

    override fun generateReference(
        classType: ClassType,
        generateSubType: GenerateType<KtType>,
    ): String? = null

    override fun canGenerate(classType: ClassType): Boolean = classType.isSubclassOf(Patch::class)
}
