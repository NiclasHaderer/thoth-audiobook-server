package io.thoth.openapi.client.kotlin.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.common.ClassType
import java.util.*

class UUIDKtGenerator : KtGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        return "UUID"
    }

    override fun insertionMode(classType: ClassType) = InsertionMode.REFERENCE

    override fun generateIdentifier(classType: ClassType, generateSubType: GenerateType): String = "UUID"

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(UUID::class)
    }
}
