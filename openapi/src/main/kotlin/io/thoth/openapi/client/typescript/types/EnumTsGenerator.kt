package io.thoth.openapi.client.typescript.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.typescript.TsTypeGenerator
import io.thoth.openapi.common.ClassType
import mu.KotlinLogging

class EnumTsGenerator : TsTypeGenerator() {
    private val log = KotlinLogging.logger {}

    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        val enumValues = classType.enumValues()
        if (enumValues?.run { isNotEmpty() } != true) {
            log.warn { "Enum type without values" }
            return "unknown"
        }

        return "type ${generateReference(classType, generateSubType)} = ${enumValues.joinToString(" | ") { "'$it'" }}"
    }

    override fun getName(classType: ClassType): String = classType.simpleName

    override fun getParsingMethod(classType: ClassType): ParseMethod = ParseMethod.TEXT

    override fun getInsertionMode(classType: ClassType) = DataType.COMPLEX

    override fun generateReference(classType: ClassType, generateSubType: GenerateType): String = classType.simpleName

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isEnum()
    }
}
