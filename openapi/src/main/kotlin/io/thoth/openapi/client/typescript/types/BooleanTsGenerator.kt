package io.thoth.openapi.client.typescript.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.typescript.TsTypeGenerator
import io.thoth.openapi.common.ClassType

class BooleanTsGenerator : TsTypeGenerator() {
    override fun generateContent(
        classType: ClassType,
        generateSubType: GenerateType<TsType>,
    ): String = "boolean"

    override fun getName(classType: ClassType): String? = null

    override fun getParsingMethod(classType: ClassType): TsParseMethod = TsParseMethod.JSON

    override fun getInsertionMode(classType: ClassType) = TsDataType.PRIMITIVE

    override fun generateReference(
        classType: ClassType,
        generateSubType: GenerateType<TsType>,
    ): String? = null

    override fun canGenerate(classType: ClassType): Boolean = classType.isSubclassOf(Boolean::class)
}
