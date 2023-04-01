package io.thoth.generators.types

import io.thoth.openapi.schema.ClassType
import kotlin.String

class StringTsGenerator : TsGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        return "string"
    }

    override fun shouldInline(classType: ClassType): Boolean = true

    override fun generateName(classType: ClassType): String = "string"

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(
            String::class,
            Char::class,
        )
    }
}
