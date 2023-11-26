package io.thoth.openapi.client.kotlin.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.common.ClassType
import java.util.*

class DateKtGenerator : KtGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        return "Date"
    }

    override fun insertionMode(classType: ClassType) = InsertionMode.INLINE

    override fun generateIdentifier(classType: ClassType, generateSubType: GenerateType): String = "Date"

    override fun withImports(classType: ClassType): List<String> {
        return listOf(
            "import java.util.Date",
        )
    }

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(
            Date::class,
        )
    }
}
