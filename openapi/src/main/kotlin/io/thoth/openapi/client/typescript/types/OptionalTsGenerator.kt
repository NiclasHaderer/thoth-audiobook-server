package io.thoth.openapi.client.typescript.types

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.typescript.TsTypeGenerator
import io.thoth.openapi.common.ClassType
import java.util.Optional

class OptionalTsGenerator : TsTypeGenerator() {
    private val log = logger {}

    override fun generateContent(
        classType: ClassType,
        generateSubType: GenerateType<TsType>,
    ): String {
        if (classType.genericArguments.isEmpty()) {
            log.warn { "Optional type without generic arguments" }
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

    override fun getName(classType: ClassType): String = "Optional"

    override fun canGenerate(classType: ClassType): Boolean = classType.isSubclassOf(Optional::class)
}
