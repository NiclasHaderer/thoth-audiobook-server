package io.thoth.generators.typescript.types

import io.thoth.generators.common.ClassType
import io.thoth.generators.openapi.responses.BinaryResponse
import io.thoth.generators.openapi.responses.FileResponse

class BinaryTsGenerator : TsGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: (classType: ClassType) -> Type): String {
        return "Blob"
    }

    override fun parseMethod(classType: ClassType): ParseMethod = ParseMethod.BLOB

    override fun insertionMode(classType: ClassType) = InsertionMode.INLINE

    override fun generateName(classType: ClassType, generateSubType: GenerateType): String = "blob"

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(
            BinaryResponse::class,
            FileResponse::class,
        )
    }
}
