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

        val subType = generateSubType(classType.genericArguments.first())
        return "(${subType.reference()})[]"
    }

    override fun shouldInline(classType: ClassType): Boolean = true

    override fun generateName(classType: ClassType): String = "array"

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