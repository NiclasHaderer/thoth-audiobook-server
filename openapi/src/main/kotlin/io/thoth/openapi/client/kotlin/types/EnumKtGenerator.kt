package io.thoth.openapi.client.kotlin.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.common.ClassType
import mu.KotlinLogging

class EnumKtGenerator : KtGenerator() {
    private val log = KotlinLogging.logger {}

    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        val enumValues = classType.enumValues()
        if (enumValues?.run { !isEmpty() } != true) {
            log.warn { "Enum type without values" }
            return "Any?"
        }

        return "enum class ${classType.simpleName} {\n${
            enumValues.joinToString(separator = ",\n") { "  $it" }
        }\n}"
    }

    override fun insertionMode(classType: ClassType) = InsertionMode.REFERENCE

    override fun generateIdentifier(classType: ClassType, generateSubType: GenerateType): String = classType.simpleName

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isEnum()
    }
}
