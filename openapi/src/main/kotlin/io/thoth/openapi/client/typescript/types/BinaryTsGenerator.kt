package io.thoth.openapi.client.typescript.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.typescript.TsTypeGenerator
import io.thoth.openapi.common.ClassType
import io.thoth.openapi.ktor.responses.BinaryResponse
import io.thoth.openapi.ktor.responses.FileResponse

class BinaryTsGenerator : TsTypeGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType<TsType>) = "Blob"

    override fun getParsingMethod(classType: ClassType): TsParseMethod = TsParseMethod.BLOB

    override fun getInsertionMode(classType: ClassType) = TsDataType.PRIMITIVE

    override fun generateReference(classType: ClassType, generateSubType: GenerateType<TsType>): String? = null

    override fun getName(classType: ClassType): String? = null

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(
            BinaryResponse::class,
            FileResponse::class,
            ByteArray::class,
        )
    }
}
