package io.thoth.openapi.client.typescript.types

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.typescript.TsTypeGenerator
import io.thoth.openapi.common.ClassType
import io.thoth.openapi.common.Patch

class PatchTsGenerator : TsTypeGenerator() {
    private val log = logger {}

    override fun generateContent(
        classType: ClassType,
        generateSubType: GenerateType<TsType>,
    ): String {
        if (classType.genericArguments.isEmpty()) {
            log.warn { "Patch type without generic arguments" }
            return "unknown"
        }
        return generateSubType(classType.genericArguments[0]).reference()
    }

    override fun getParsingMethod(classType: ClassType): TsParseMethod = TsParseMethod.JSON

    override fun getInsertionMode(classType: ClassType) = TsDataType.PRIMITIVE

    override fun generateReference(
        classType: ClassType,
        generateSubType: GenerateType<TsType>,
    ): String? = null

    override fun getName(classType: ClassType): String = "Patch"

    override fun canGenerate(classType: ClassType): Boolean = classType.isSubclassOf(Patch::class)
}
