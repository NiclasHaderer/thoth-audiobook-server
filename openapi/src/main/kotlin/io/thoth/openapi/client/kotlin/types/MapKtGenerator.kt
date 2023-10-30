package io.thoth.openapi.client.kotlin.types

import io.thoth.openapi.client.common.GenerateType
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

    override fun insertionMode(classType: ClassType) = InsertionMode.INLINE

    override fun generateIdentifier(classType: ClassType, generateSubType: GenerateType): String = "Record"

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(
            Map::class,
            HashMap::class,
            LinkedHashMap::class,
        )
    }
}
