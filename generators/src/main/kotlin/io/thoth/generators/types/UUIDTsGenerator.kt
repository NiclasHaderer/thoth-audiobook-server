package io.thoth.generators.types

import io.thoth.openapi.schema.ClassType
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
