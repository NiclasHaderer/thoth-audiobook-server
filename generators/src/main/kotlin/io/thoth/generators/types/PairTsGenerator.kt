package io.thoth.generators.types

import io.thoth.openapi.schema.ClassType
import mu.KotlinLogging

class PairTsGenerator : TsGenerator() {
    private val log = KotlinLogging.logger {}

    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        if (classType.genericArguments.size != 2) {
            log.warn { "Pari type without insufficient arguments" }
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

    override fun shouldInline(classType: ClassType): Boolean = true

    override fun generateName(classType: ClassType): String = "pair"

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(
            Pair::class,
        )
    }
}
