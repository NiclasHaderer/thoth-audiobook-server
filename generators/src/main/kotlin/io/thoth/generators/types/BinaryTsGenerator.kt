package io.thoth.generators.types

import io.thoth.openapi.responses.BinaryResponse
import io.thoth.openapi.responses.FileResponse
import io.thoth.openapi.schema.ClassType

class BinaryTsGenerator : TsGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: (classType: ClassType) -> Type): String {
        return "Blob"
    }

    override fun parseMethod(classType: ClassType): ParseMethod = ParseMethod.BLOB

    override fun shouldInline(classType: ClassType): Boolean = true

    override fun generateName(classType: ClassType): String = "blob"

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(
            BinaryResponse::class,
            FileResponse::class,
        )
    }
}
