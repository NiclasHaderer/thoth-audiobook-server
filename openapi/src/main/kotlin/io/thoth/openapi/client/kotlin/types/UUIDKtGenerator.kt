package io.thoth.openapi.client.kotlin.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.kotlin.KtGenerator
import io.thoth.openapi.common.ClassType
import java.util.*

class UUIDKtGenerator : KtGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        return "UUID"
    }

    override fun getInsertionMode(classType: ClassType) = DataType.PRIMITIVE

    override fun generateReference(classType: ClassType, generateSubType: GenerateType): String = "UUID"

    override fun withImports(classType: ClassType): List<String> {
        return listOf(
            "import java.util.UUID",
        )
    }

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(UUID::class)
    }
}
