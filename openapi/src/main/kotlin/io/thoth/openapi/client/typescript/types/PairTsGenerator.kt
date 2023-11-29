package io.thoth.openapi.client.typescript.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.common.TypeGenerator
import io.thoth.openapi.client.typescript.TsGenerator
import io.thoth.openapi.common.ClassType
import mu.KotlinLogging

class PairTsGenerator : TsGenerator() {
    private val log = KotlinLogging.logger {}

    override fun generateContent(
        classType: ClassType,
        generateSubType: (classType: ClassType) -> TypeGenerator.Type
    ): String {
        if (classType.genericArguments.size != 2) {
            log.warn { "Pair type without insufficient arguments" }
            return "[unknown, unknown]"
        }

        val genericArg1 = classType.genericArguments.first()
        val subType1 = generateSubType(genericArg1)
        val genericArg2 = classType.genericArguments.last()
        val subType2 = generateSubType(genericArg2)

        return "[${subType1.reference()} ${
            if (genericArg1.isNullable) {
                " | undefined"
            } else {
                ""
            }
        }, ${subType2.reference()} ${
            if (genericArg2.isNullable) {
                " | undefined"
            } else {
                ""
            }
        }]"
    }

    override fun parseMethod(classType: ClassType): ParseMethod = ParseMethod.JSON

    override fun insertionMode(classType: ClassType) = InsertionMode.INLINE

    override fun generateIdentifier(classType: ClassType, generateSubType: GenerateType): String? = null

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(
            Pair::class,
        )
    }
}
