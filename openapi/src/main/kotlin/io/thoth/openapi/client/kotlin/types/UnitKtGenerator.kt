package io.thoth.openapi.client.kotlin.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.kotlin.KtGenerator
import io.thoth.openapi.common.ClassType

class UnitKtGenerator : KtGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        return "Unit"
    }

    override fun getInsertionMode(classType: ClassType) = DataType.PRIMITIVE

    override fun generateReference(classType: ClassType, generateSubType: GenerateType): String = "Unit"

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(Unit::class)
    }
}
