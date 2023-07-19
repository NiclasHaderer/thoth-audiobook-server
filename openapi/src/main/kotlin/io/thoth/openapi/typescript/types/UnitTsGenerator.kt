package io.thoth.openapi.typescript.types

import io.thoth.openapi.common.ClassType

class UnitTsGenerator : TsGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        return """type Empty = "" """
    }

    override fun parseMethod(classType: ClassType): ParseMethod = ParseMethod.TEXT

    override fun insertionMode(classType: ClassType) = InsertionMode.REFERENCE

    override fun generateName(classType: ClassType, generateSubType: GenerateType): String = "Empty"

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(Unit::class)
    }
}
