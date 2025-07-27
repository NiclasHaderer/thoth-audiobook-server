package io.thoth.openapi.client.typescript.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.typescript.TsTypeGenerator
import io.thoth.openapi.common.ClassType
import mu.KotlinLogging

class PairTsGenerator : TsTypeGenerator() {
    private val log = KotlinLogging.logger {}

    override fun generateContent(classType: ClassType, generateSubType: GenerateType<TsType>): String {
        if (classType.genericArguments.size != 2) {
            log.warn { "Pair type without insufficient arguments" }
            return "Pair<unknown, unknown>"
        }

        val genericArg1 = classType.genericArguments.first()
        val subType1 = generateSubType(genericArg1)
        val genericArg2 = classType.genericArguments.last()
        val subType2 = generateSubType(genericArg2)

        return "Pair<${subType1.reference()} ${
            if (genericArg1.isNullable) {
                " | null"
            } else {
                ""
            }
        }, ${subType2.reference()} ${
            if (genericArg2.isNullable) {
                " | null"
            } else {
                ""
            }
        }>"
    }

    override fun getParsingMethod(classType: ClassType): TsParseMethod = TsParseMethod.JSON

    override fun getInsertionMode(classType: ClassType) = TsDataType.PRIMITIVE

    override fun generateReference(classType: ClassType, generateSubType: GenerateType<TsType>): String? = null

    override fun getName(classType: ClassType): String = "Pair"

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(Pair::class)
    }
}
