package io.thoth.openapi.client.typescript.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.typescript.TsTypeGenerator
import io.thoth.openapi.common.ClassType

class BooleanTsGenerator : TsTypeGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String = "boolean"

    override fun getName(classType: ClassType): String? = null

    override fun getParsingMethod(classType: ClassType): ParseMethod = ParseMethod.JSON

    override fun getInsertionMode(classType: ClassType) = DataType.PRIMITIVE

    override fun generateReference(classType: ClassType, generateSubType: GenerateType): String? = null

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(Boolean::class)
    }
}
