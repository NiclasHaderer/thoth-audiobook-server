package io.thoth.generators.types

import io.thoth.openapi.schema.ClassType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class DateTsGenerator : TsGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        return "string"
    }

    override fun shouldInline(classType: ClassType): Boolean = true

    override fun generateName(classType: ClassType): String = "string"

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(
            Date::class,
            LocalDate::class,
            LocalDateTime::class,
        )
    }
}
