package io.thoth.openapi.client.typescript.types

import io.thoth.openapi.common.ClassType
import mu.KotlinLogging.logger

class RecordTsGenerator : TsGenerator() {
    private val log = logger {}

    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {

        if (classType.genericArguments.size != 2) {
            log.warn { "Record type without generic arguments" }
            return "Record<unknown, unknown>"
        }

        val keyClassType = classType.genericArguments[0]
        val keyType = generateSubType(keyClassType)
        val valueClassType = classType.genericArguments[1]
        val valueType = generateSubType(valueClassType)

        return "Record<${keyType.reference()} ${
            if (keyClassType.isNullable) {
                " | undefined"
            } else {
                ""
            }
        }, ${valueType.reference()}>"
    }

    override fun parseMethod(classType: ClassType): ParseMethod = ParseMethod.JSON

    override fun insertionMode(classType: ClassType) = InsertionMode.INLINE

    override fun generateName(classType: ClassType, generateSubType: GenerateType): String = "Record"

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(
            Map::class,
            HashMap::class,
            LinkedHashMap::class,
        )
    }
}
