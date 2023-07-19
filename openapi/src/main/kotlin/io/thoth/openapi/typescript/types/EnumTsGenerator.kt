package io.thoth.openapi.typescript.types

import io.thoth.openapi.common.ClassType
import mu.KotlinLogging

class EnumTsGenerator : TsGenerator() {
    private val log = KotlinLogging.logger {}

    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        val enumValues = classType.enumValues()
        if (enumValues?.run { !isEmpty() } != true) {
            log.warn { "Enum type without values" }
            return "unknown"
        }

        return "type ${generateName(classType, generateSubType)} = ${enumValues.joinToString(" | ") { "'$it'" }}"
    }

    override fun parseMethod(classType: ClassType): ParseMethod = ParseMethod.TEXT

    override fun insertionMode(classType: ClassType) = InsertionMode.REFERENCE
    override fun generateName(classType: ClassType, generateSubType: GenerateType): String = classType.simpleName
    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isEnum()
    }
}
