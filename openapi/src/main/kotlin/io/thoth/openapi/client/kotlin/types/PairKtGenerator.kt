package io.thoth.openapi.client.kotlin.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.common.TypeGenerator
import io.thoth.openapi.client.kotlin.KtGenerator
import io.thoth.openapi.common.ClassType
import mu.KotlinLogging

class PairKtGenerator : KtGenerator() {
    private val log = KotlinLogging.logger {}

    override fun generateContent(
        classType: ClassType,
        generateSubType: (classType: ClassType) -> TypeGenerator.Type
    ): String {
        if (classType.genericArguments.size != 2) {
            log.warn { "Pair type without insufficient arguments" }
            return "Pair<*,*>"
        }

        val genericArg1 = classType.genericArguments.first()
        val subType1 = generateSubType(genericArg1)
        val genericArg2 = classType.genericArguments.last()
        val subType2 = generateSubType(genericArg2)

        return "Pair<${subType1.reference()} ${
            if (genericArg1.isNullable) {
                "?"
            } else {
                ""
            }
        }, ${subType2.reference()} ${
            if (genericArg2.isNullable) {
                "?"
            } else {
                ""
            }
        }>"
    }

    override fun getName(classType: ClassType): String? = null

    override fun getInsertionMode(classType: ClassType) = DataType.PRIMITIVE

    override fun generateReference(classType: ClassType, generateSubType: GenerateType): String? = null

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(
            Pair::class,
        )
    }
}
