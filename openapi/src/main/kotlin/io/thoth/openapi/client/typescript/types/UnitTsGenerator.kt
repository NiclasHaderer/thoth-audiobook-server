package io.thoth.openapi.client.typescript.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.typescript.TsTypeGenerator
import io.thoth.openapi.common.ClassType

class UnitTsGenerator : TsTypeGenerator() {
    override fun generateContent(
        classType: ClassType,
        generateSubType: GenerateType<TsType>,
    ): String = """type Empty = "" """

    override fun getParsingMethod(classType: ClassType): TsParseMethod = TsParseMethod.TEXT

    override fun getInsertionMode(classType: ClassType) = TsDataType.COMPLEX

    override fun generateReference(
        classType: ClassType,
        generateSubType: GenerateType<TsType>,
    ): String = "Empty"

    override fun getName(classType: ClassType): String = "Empty"

    override fun canGenerate(classType: ClassType): Boolean = classType.isSubclassOf(Unit::class)
}
