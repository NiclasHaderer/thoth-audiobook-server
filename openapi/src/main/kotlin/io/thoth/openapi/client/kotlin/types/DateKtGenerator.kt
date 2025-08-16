package io.thoth.openapi.client.kotlin.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.kotlin.KtTypeGenerator
import io.thoth.openapi.common.ClassType
import java.util.*

class DateKtGenerator : KtTypeGenerator() {
    override fun generateContent(
        classType: ClassType,
        generateSubType: GenerateType<KtType>,
    ): String = "Date"

    override fun getName(classType: ClassType): String? = null

    override fun getInsertionMode(classType: ClassType) = KtDataType.PRIMITIVE

    override fun generateReference(
        classType: ClassType,
        generateSubType: GenerateType<KtType>,
    ): String? = null

    override fun withImports(
        classType: ClassType,
        generateSubType: GenerateType<KtType>,
    ): List<String> = listOf("import java.util.Date")

    override fun canGenerate(classType: ClassType): Boolean = classType.isSubclassOf(Date::class)
}
