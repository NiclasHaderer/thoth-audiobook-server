package io.thoth.openapi.client.kotlin.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.kotlin.KtTypeGenerator
import io.thoth.openapi.common.ClassType
import mu.KotlinLogging

class EnumKtGenerator : KtTypeGenerator() {
    private val log = KotlinLogging.logger {}

    override fun generateContent(classType: ClassType, generateSubType: GenerateType<KtType>): String {
        val enumValues = classType.enumValues()
        if (enumValues?.run { isNotEmpty() } != true) {
            log.warn { "Enum type without values" }
            return "Any?"
        }

        return "enum class ${classType.simpleName} {\n${
            enumValues.joinToString(separator = ",\n") { "  $it" }
        }\n}"
    }

    override fun getName(classType: ClassType): String = classType.simpleName

    override fun getInsertionMode(classType: ClassType) = KtDataType.COMPLEX

    override fun generateReference(classType: ClassType, generateSubType: GenerateType<KtType>): String =
        classType.simpleName

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isEnum()
    }
}
