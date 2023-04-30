package io.thoth.generators.typescript.types

import io.thoth.generators.common.ClassType
import kotlin.String

class StringTsGenerator : TsGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        return "string"
    }

    override fun parseMethod(classType: ClassType): ParseMethod = ParseMethod.TEXT

    override fun insertionMode(classType: ClassType) = InsertionMode.INLINE

    override fun generateName(classType: ClassType, generateSubType: GenerateType): String = "string"

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(
            String::class,
            Char::class,
        )
    }
}