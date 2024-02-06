package io.thoth.openapi.client.typescript.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.typescript.TsTypeGenerator
import io.thoth.openapi.common.ClassType
import kotlin.String

class StringTsGenerator : TsTypeGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType<TsType>): String {
        return "string"
    }

    override fun getParsingMethod(classType: ClassType): TsParseMethod = TsParseMethod.TEXT

    override fun getInsertionMode(classType: ClassType) = TsDataType.PRIMITIVE

    override fun generateReference(classType: ClassType, generateSubType: GenerateType<TsType>): String? = null

    override fun getName(classType: ClassType): String? = null

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(
            String::class,
            Char::class,
        )
    }
}
