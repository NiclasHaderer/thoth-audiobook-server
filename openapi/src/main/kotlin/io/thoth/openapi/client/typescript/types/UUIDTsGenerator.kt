package io.thoth.openapi.client.typescript.types

import io.thoth.openapi.common.ClassType
import java.util.*

class UUIDTsGenerator : TsGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        return "type UUID = `\${string}-\${string}-\${string}-\${string}-\${string}`"
    }

    override fun parseMethod(classType: ClassType): ParseMethod = ParseMethod.TEXT

    override fun insertionMode(classType: ClassType) = InsertionMode.REFERENCE

    override fun generateName(classType: ClassType, generateSubType: GenerateType): String = "UUID"

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(UUID::class)
    }
}
