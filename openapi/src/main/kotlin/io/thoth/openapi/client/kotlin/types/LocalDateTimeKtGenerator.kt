package io.thoth.openapi.client.kotlin.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.common.ClassType
import java.time.LocalDateTime
import java.util.*

class LocalDateTimeKtGenerator : KtGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        return "LocalDateTime"
    }

    override fun insertionMode(classType: ClassType) = InsertionMode.INLINE

    override fun generateIdentifier(classType: ClassType, generateSubType: GenerateType): String = "LocalDateTime"

    override fun withImports(classType: ClassType): List<String> {
        return listOf(
            "import java.time.LocalDateTime",
        )
    }

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(
            LocalDateTime::class,
        )
    }
}
