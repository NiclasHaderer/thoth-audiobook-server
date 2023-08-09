package io.thoth.openapi.typescript.types

import io.thoth.openapi.common.ClassType

class AnyTsGenerator : TsGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        return "any"
    }

    override fun priority(classType: ClassType): Int {
        return -1
    }

    override fun parseMethod(classType: ClassType): ParseMethod = ParseMethod.JSON

    override fun insertionMode(classType: ClassType) = InsertionMode.INLINE

    override fun generateName(classType: ClassType, generateSubType: GenerateType): String = "any"

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.clazz == Any::class
    }
}
