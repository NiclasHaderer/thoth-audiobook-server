package io.thoth.openapi.client.typescript.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.typescript.TsTypeGenerator
import io.thoth.openapi.common.ClassType
import java.util.*

class UUIDTsGenerator : TsTypeGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType<TsType>): String {
        return "type UUID = `\${string}-\${string}-\${string}-\${string}-\${string}`"
    }

    override fun getParsingMethod(classType: ClassType): TsParseMethod = TsParseMethod.TEXT

    override fun getInsertionMode(classType: ClassType) = TsDataType.COMPLEX

    override fun generateReference(classType: ClassType, generateSubType: GenerateType<TsType>): String = "UUID"

    override fun getName(classType: ClassType): String = "UUID"

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(UUID::class)
    }
}
