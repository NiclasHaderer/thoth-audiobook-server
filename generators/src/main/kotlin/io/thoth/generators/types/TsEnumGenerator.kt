package io.thoth.generators.types

import io.thoth.openapi.schema.ClassType
import mu.KotlinLogging

class TsEnumGenerator : TsGenerator() {
    private val log = KotlinLogging.logger {}

    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        val enumValues = classType.clazz.java.enumConstants
        if (enumValues.isEmpty()) {
            log.warn { "Enum type without values" }
            return "unknown"
        }

        return "${generateName(classType)} = ${enumValues.joinToString(" | ") { "'$it'" }}"
    }

    override fun shouldInline(classType: ClassType): Boolean = false
    override fun generateName(classType: ClassType): String = classType.clazz.simpleName!!
    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isEnum
    }
}
