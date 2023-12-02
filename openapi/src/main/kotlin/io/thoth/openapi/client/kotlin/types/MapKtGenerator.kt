package io.thoth.openapi.client.kotlin.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.kotlin.KtGenerator
import io.thoth.openapi.common.ClassType
import mu.KotlinLogging.logger

class MapKtGenerator : KtGenerator() {
    private val log = logger {}

    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        if (classType.genericArguments.size != 2) {
            log.warn { "Record type without generic arguments" }
            return "Map<*, *>"
        }

        val keyClassType = classType.genericArguments[0]
        val keyType = generateSubType(keyClassType)
        val valueClassType = classType.genericArguments[1]
        val valueType = generateSubType(valueClassType)

        val className = classType.simpleName

        return "${className}<${keyType.reference()} ${
            if (keyClassType.isNullable) {
                "?"
            } else {
                ""
            }
        }, ${valueType.reference()}>"
    }

    override fun getName(classType: ClassType): String = "Map"

    override fun getInsertionMode(classType: ClassType) = DataType.PRIMITIVE

    override fun generateReference(classType: ClassType, generateSubType: GenerateType): String? = null

    override fun withImports(classType: ClassType, generateSubType: GenerateType): List<String> {
        if (classType.genericArguments.size != 2) {
            return listOf()
        }
        return listOf(
            classType.genericArguments[0].clazz.qualifiedName ?: "",
            classType.genericArguments[1].clazz.qualifiedName ?: "",
        )
            .filter { it.isNotEmpty() }
    }

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(
            Map::class,
            HashMap::class,
            LinkedHashMap::class,
        )
    }
}
