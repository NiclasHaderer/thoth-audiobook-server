package io.thoth.openapi.client.kotlin.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.kotlin.KtTypeGenerator
import io.thoth.openapi.common.ClassType
import io.thoth.openapi.ktor.responses.BinaryResponse
import io.thoth.openapi.ktor.responses.FileResponse

class BinaryKtGenerator : KtTypeGenerator() {
    override fun generateContent(
        classType: ClassType,
        generateSubType: GenerateType<KtType>,
    ): String {
        return "ByteArrayInputStream"
    }

    override fun getName(classType: ClassType): String? = null

    override fun getInsertionMode(classType: ClassType) = KtDataType.PRIMITIVE

    override fun generateReference(classType: ClassType, generateSubType: GenerateType<KtType>): String? = null

    override fun withImports(classType: ClassType, generateSubType: GenerateType<KtType>): List<String> {
        return listOf(
            "import java.io.ByteArrayInputStream",
        )
    }

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(
            BinaryResponse::class,
            FileResponse::class,
            ByteArray::class,
        )
    }
}
