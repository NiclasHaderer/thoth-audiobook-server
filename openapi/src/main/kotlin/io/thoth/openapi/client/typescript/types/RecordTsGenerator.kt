package io.thoth.openapi.client.typescript.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.typescript.TsTypeGenerator
import io.thoth.openapi.common.ClassType
import mu.KotlinLogging.logger

class RecordTsGenerator : TsTypeGenerator() {
    private val log = logger {}

    override fun generateContent(classType: ClassType, generateSubType: GenerateType<TsType>): String {

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

    override fun getParsingMethod(classType: ClassType): TsParseMethod = TsParseMethod.JSON

    override fun getInsertionMode(classType: ClassType) = TsDataType.PRIMITIVE

    override fun generateReference(classType: ClassType, generateSubType: GenerateType<TsType>): String? = null

    override fun getName(classType: ClassType): String = "Record"

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(Map::class, HashMap::class, LinkedHashMap::class)
    }
}
