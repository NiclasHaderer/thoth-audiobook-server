package io.thoth.openapi.client.kotlin.types

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.kotlin.KtTypeGenerator
import io.thoth.openapi.common.ClassType
import java.util.Optional

class OptionalKtGenerator : KtTypeGenerator() {
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
        if (classType.genericArguments.isEmpty()) {
            log.warn { "Optional type without generic arguments" }
            return "Optional<*>"
        }
        val value = generateSubType(classType.genericArguments[0])
        return "Optional<${if (impl) value.referenceImpl() else value.reference()}>"
    }

    override fun withImports(
        classType: ClassType,
        generateSubType: GenerateType<KtType>,
    ): List<String> =
        listOf("import java.util.Optional") + classType.genericArguments.flatMap { generateSubType(it).imports() }

    override fun getInsertionMode(classType: ClassType) = KtDataType.PRIMITIVE

    override fun getName(classType: ClassType): String = "Optional"

    override fun generateReference(
        classType: ClassType,
        generateSubType: GenerateType<KtType>,
    ): String? = null

    override fun canGenerate(classType: ClassType): Boolean = classType.isSubclassOf(Optional::class)
}
