package io.thoth.openapi.client.kotlin.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.kotlin.KtTypeGenerator
import io.thoth.openapi.common.ClassType
import mu.KotlinLogging

class ArrayKtGenerator : KtTypeGenerator() {
    private val log = KotlinLogging.logger {}

    override fun generateContent(classType: ClassType, generateSubType: GenerateType<KtType>): String {
        if (classType.genericArguments.isEmpty()) {
            log.warn { "Array type without generic arguments" }
            return "List<*>"
        }

        val genericArg = classType.genericArguments.first()
        val subType = generateSubType(genericArg)

        return buildString {
            append("List<")
            append(subType.reference())
            if (genericArg.isNullable) {
                append("?")
            }
            append(">")
        }
    }

    override fun withImports(classType: ClassType, generateSubType: GenerateType<KtType>): List<String> {
        return classType.genericArguments.flatMap { generateSubType(it).imports() }
    }

    override fun getInsertionMode(classType: ClassType) = KtDataType.PRIMITIVE

    override fun getName(classType: ClassType): String = "List"

    override fun generateReference(classType: ClassType, generateSubType: GenerateType<KtType>): String? = null

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
