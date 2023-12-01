package io.thoth.openapi.client.kotlin.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.common.TypeGenerator
import io.thoth.openapi.client.kotlin.KtGenerator
import io.thoth.openapi.common.ClassType
import io.thoth.openapi.ktor.responses.BinaryResponse
import io.thoth.openapi.ktor.responses.FileResponse

class BinaryKtGenerator : KtGenerator() {
    override fun generateContent(
        classType: ClassType,
        generateSubType: (classType: ClassType) -> TypeGenerator.Type
    ): String {
        return "ByteArrayInputStream"
    }

    override fun getName(classType: ClassType): String? = null

    override fun getInsertionMode(classType: ClassType) = DataType.PRIMITIVE

    override fun generateReference(classType: ClassType, generateSubType: GenerateType): String? = null

    override fun withImports(classType: ClassType, generateSubType: GenerateType): List<String> {
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
