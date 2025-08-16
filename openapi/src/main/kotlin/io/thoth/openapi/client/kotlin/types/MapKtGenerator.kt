package io.thoth.openapi.client.kotlin.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.kotlin.KtTypeGenerator
import io.thoth.openapi.common.ClassType
import mu.KotlinLogging.logger

class MapKtGenerator : KtTypeGenerator() {
    private val log = logger {}

    override fun generateContent(
        classType: ClassType,
        generateSubType: GenerateType<KtType>,
    ): String {
        if (classType.genericArguments.size != 2) {
            log.warn { "Record type without generic arguments" }
            return "Map<*, *>"
        }

        val keyClassType = classType.genericArguments[0]
        val keyType = generateSubType(keyClassType)
        val valueClassType = classType.genericArguments[1]
        val valueType = generateSubType(valueClassType)

        val className = classType.simpleName

        return "$className<${keyType.reference()} ${
            if (keyClassType.isNullable) {
                "?"
            } else {
                ""
            }
        }, ${valueType.reference()}>"
    }

    override fun getName(classType: ClassType): String = "Map"

    override fun getInsertionMode(classType: ClassType) = KtDataType.PRIMITIVE

    override fun generateReference(
        classType: ClassType,
        generateSubType: GenerateType<KtType>,
    ): String? = null

    override fun withImports(
        classType: ClassType,
        generateSubType: GenerateType<KtType>,
    ): List<String> =
        classType.genericArguments.flatMap {
            generateSubType(it).imports()
        }

    override fun canGenerate(classType: ClassType): Boolean =
        classType.isSubclassOf(Map::class, HashMap::class, LinkedHashMap::class)
}
