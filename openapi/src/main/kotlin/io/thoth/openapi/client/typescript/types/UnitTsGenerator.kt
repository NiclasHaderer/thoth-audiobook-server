package io.thoth.openapi.client.typescript.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.typescript.TsGenerator
import io.thoth.openapi.common.ClassType

class UnitTsGenerator : TsGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        return """type Empty = "" """
    }

    override fun getParsingMethod(classType: ClassType): ParseMethod = ParseMethod.TEXT

    override fun getInsertionMode(classType: ClassType) = DataType.COMPLEX

    override fun generateReference(classType: ClassType, generateSubType: GenerateType): String = "Empty"
    override fun getName(classType: ClassType): String? = null
    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(Unit::class)
    }
}
