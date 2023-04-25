package io.thoth.generators.types

import io.thoth.openapi.schema.ClassType
import mu.KotlinLogging

class ArrayTsGenerator : TsGenerator() {
    private val log = KotlinLogging.logger {}

    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        if (classType.genericArguments.isEmpty()) {
            log.warn { "Array type without generic arguments" }
            return "unknown[]"
        }

        val genericArg = classType.genericArguments.first()
        val subType = generateSubType(genericArg)
        return "(${subType.reference()}${
            if (genericArg.isNullable) {
                " | null"
            } else {
                ""
            }
        })[]"
    }

    override fun parseMethod(classType: ClassType): ParseMethod = ParseMethod.JSON

    override fun insertionMode(classType: ClassType) = InsertionMode.INLINE

    override fun generateName(classType: ClassType, generateSubType: GenerateType): String = "Array"

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
