package io.thoth.openapi.client.kotlin.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.kotlin.KtGenerator
import io.thoth.openapi.common.ClassType
import mu.KotlinLogging

class ArrayKtGenerator : KtGenerator() {
    private val log = KotlinLogging.logger {}

    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        if (classType.genericArguments.isEmpty()) {
            log.warn { "Array type without generic arguments" }
            return "List<*>"
        }

        val genericArg = classType.genericArguments.first()
        val subType = generateSubType(genericArg)
        val content =
            "List<${subType.reference()}${
            if (genericArg.isNullable) {
                "?"
            } else {
                ""
            }
        }>"
        return content
    }

    override fun getInsertionMode(classType: ClassType) = DataType.PRIMITIVE

    override fun getName(classType: ClassType): String? = null

    override fun generateReference(classType: ClassType, generateSubType: GenerateType): String? = null

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
