package io.thoth.openapi.client.kotlin.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.kotlin.KtGenerator
import io.thoth.openapi.common.ClassType
import java.time.LocalDate

class LocalDateKtGenerator : KtGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        return "LocalDate"
    }

    override fun getInsertionMode(classType: ClassType) = DataType.PRIMITIVE

    override fun generateReference(classType: ClassType, generateSubType: GenerateType): String = "LocalDate"

    override fun withImports(classType: ClassType): List<String> {
        return listOf(
            "import java.time.LocalDate",
        )
    }

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(
            LocalDate::class,
        )
    }
}
