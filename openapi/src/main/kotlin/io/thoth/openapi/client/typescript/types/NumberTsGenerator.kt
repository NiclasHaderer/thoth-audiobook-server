package io.thoth.openapi.client.typescript.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.typescript.TsTypeGenerator
import io.thoth.openapi.common.ClassType
import java.math.BigDecimal
import java.math.BigInteger

class NumberTsGenerator : TsTypeGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType<TsType>): String {
        return "number"
    }

    override fun getParsingMethod(classType: ClassType): TsParseMethod = TsParseMethod.TEXT

    override fun getInsertionMode(classType: ClassType) = TsDataType.PRIMITIVE

    override fun generateReference(classType: ClassType, generateSubType: GenerateType<TsType>): String? = null

    override fun getName(classType: ClassType): String? = null

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(
            Number::class,
            Int::class,
            Long::class,
            Double::class,
            Float::class,
            Short::class,
            Byte::class,
            BigDecimal::class,
            BigInteger::class,
            UInt::class,
            ULong::class,
            UShort::class,
            UByte::class,
        )
    }
}
