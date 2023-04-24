package io.thoth.generators.types

import io.thoth.openapi.responses.RedirectResponse
import io.thoth.openapi.schema.ClassType

class RedirectTsGenerator : TsGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: (classType: ClassType) -> Type): String {
        return "string"
    }

    override fun parseMethod(classType: ClassType): ParseMethod = ParseMethod.TEXT

    override fun insertionMode(classType: ClassType) = InsertionMode.INLINE

    override fun generateName(classType: ClassType, generateSubType: GenerateType): String = "redirect"

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(
            RedirectResponse::class,
        )
    }
}
