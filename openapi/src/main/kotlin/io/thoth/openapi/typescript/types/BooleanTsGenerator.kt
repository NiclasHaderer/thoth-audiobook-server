package io.thoth.openapi.typescript.types

import io.thoth.openapi.common.ClassType

class BooleanTsGenerator : TsGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        return "boolean"
    }

    override fun parseMethod(classType: ClassType): ParseMethod = ParseMethod.JSON

    override fun insertionMode(classType: ClassType) = InsertionMode.INLINE

    override fun generateName(classType: ClassType, generateSubType: GenerateType): String = "boolean"

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(Boolean::class)
    }
}
