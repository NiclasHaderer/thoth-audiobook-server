package io.thoth.openapi.client.typescript.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.typescript.TsGenerator
import io.thoth.openapi.common.ClassType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class DateTsGenerator : TsGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String = "string"

    override fun getName(classType: ClassType): String? = null

    override fun getParsingMethod(classType: ClassType): ParseMethod = ParseMethod.TEXT

    override fun getInsertionMode(classType: ClassType) = DataType.PRIMITIVE

    override fun generateReference(classType: ClassType, generateSubType: GenerateType): String? = null

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(
            Date::class,
            LocalDate::class,
            LocalDateTime::class,
        )
    }
}
