package io.thoth.generators.types

import io.thoth.openapi.schema.ClassType

class BooleanTsGenerator : TsGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        return "boolean"
    }

    override fun parseMethod(classType: ClassType): ParseMethod = ParseMethod.JSON

    override fun shouldInline(classType: ClassType): Boolean = true

    override fun generateName(classType: ClassType): String = "boolean"

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(Boolean::class)
    }
}
