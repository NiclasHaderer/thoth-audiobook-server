package io.thoth.generators.types

import io.thoth.openapi.schema.ClassType

class ByteArrayTsGenerator : TsGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        return "Uint8Array"
    }

    override fun shouldInline(classType: ClassType): Boolean = true

    override fun generateName(classType: ClassType): String = "Uint8Array"

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(ByteArray::class)
    }
}
