package io.thoth.generators.typescript.types

import io.thoth.generators.common.ClassType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class DateTsGenerator : TsGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        return "string"
    }

    override fun parseMethod(classType: ClassType): ParseMethod = ParseMethod.TEXT

    override fun insertionMode(classType: ClassType) = InsertionMode.INLINE

    override fun generateName(classType: ClassType, generateSubType: GenerateType): String = "string"

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(
            Date::class,
            LocalDate::class,
            LocalDateTime::class,
        )
    }
}
