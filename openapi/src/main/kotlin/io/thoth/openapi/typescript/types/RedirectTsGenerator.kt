package io.thoth.openapi.typescript.types

import io.thoth.openapi.common.ClassType
import io.thoth.openapi.ktor.responses.RedirectResponse

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