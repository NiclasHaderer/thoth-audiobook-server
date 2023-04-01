package io.thoth.generators.types

import io.thoth.openapi.schema.ClassType
import mu.KotlinLogging.logger

class RecordTsGenerator : TsGenerator() {
    private val log = logger {}
    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {

        if (classType.genericArguments.size != 2) {
            log.warn { "Record type without generic arguments" }
            return "Record<unknown, unknown>"
        }

        val keyType = generateSubType(classType.genericArguments[0])
        val valueType = generateSubType(classType.genericArguments[1])

        return "Record<${keyType.reference()}, ${valueType.reference()}>"
    }

    override fun shouldInline(classType: ClassType): Boolean = true

    override fun generateName(classType: ClassType): String = "Record"

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(
            Map::class,
            HashMap::class,
            LinkedHashMap::class,
        )
    }
}
