package io.thoth.openapi.client.typescript.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.typescript.TsGenerator
import io.thoth.openapi.common.ClassType
import mu.KotlinLogging

class ArrayTsGenerator : TsGenerator() {
    private val log = KotlinLogging.logger {}

    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        if (classType.genericArguments.isEmpty()) {
            log.warn { "Array type without generic arguments" }
            return "Array<unknown>"
        }

        val genericArg = classType.genericArguments.first()
        val subType = generateSubType(genericArg)

        return buildString {
            append("Array<")
            append(subType.reference())
            if (genericArg.isNullable) {
                append(" | null")
            }
            append(">")
        }
    }

    override fun getParsingMethod(classType: ClassType): ParseMethod = ParseMethod.JSON

    override fun getInsertionMode(classType: ClassType) = DataType.PRIMITIVE

    override fun generateReference(classType: ClassType, generateSubType: GenerateType): String? = null

    override fun getName(classType: ClassType): String = "Array"

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(
            List::class,
            Array::class,
            Collection::class,
            ArrayList::class,
            HashSet::class,
            LinkedHashSet::class,
            Set::class,
        )
    }
}
