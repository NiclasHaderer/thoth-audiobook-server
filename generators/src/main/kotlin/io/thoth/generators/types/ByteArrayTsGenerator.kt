package io.thoth.generators.types

import io.thoth.openapi.schema.ClassType

class ByteArrayTsGenerator : TsGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        return "Uint8Array"
    }

    override fun parseMethod(classType: ClassType): ParseMethod = ParseMethod.BLOB

    override fun insertionMode(classType: ClassType) = InsertionMode.INLINE

    override fun generateName(classType: ClassType, generateSubType: GenerateType): String = "Uint8Array"

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(ByteArray::class)
    }
}
