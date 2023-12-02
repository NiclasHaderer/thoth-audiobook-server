package io.thoth.openapi.client.typescript.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.typescript.TsGenerator
import io.thoth.openapi.common.ClassType

class AnyTsGenerator : TsGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String = "any"

    override fun getName(classType: ClassType): String? = null

    override fun getParsingMethod(classType: ClassType): ParseMethod = ParseMethod.JSON

    override fun getInsertionMode(classType: ClassType) = DataType.PRIMITIVE

    override fun canGenerate(classType: ClassType): Boolean = classType.clazz == Any::class

    override fun generateReference(classType: ClassType, generateSubType: GenerateType): String? = null
}