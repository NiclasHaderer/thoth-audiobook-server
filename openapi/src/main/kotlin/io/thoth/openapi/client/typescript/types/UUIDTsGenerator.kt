package io.thoth.openapi.client.typescript.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.typescript.TsGenerator
import io.thoth.openapi.common.ClassType
import java.util.*

class UUIDTsGenerator : TsGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        return "type UUID = `\${string}-\${string}-\${string}-\${string}-\${string}`"
    }

    override fun getParsingMethod(classType: ClassType): ParseMethod = ParseMethod.TEXT

    override fun getInsertionMode(classType: ClassType) = DataType.COMPLEX

    override fun generateReference(classType: ClassType, generateSubType: GenerateType): String = "UUID"
    override fun getName(classType: ClassType): String = "UUID"

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(UUID::class)
    }
}
