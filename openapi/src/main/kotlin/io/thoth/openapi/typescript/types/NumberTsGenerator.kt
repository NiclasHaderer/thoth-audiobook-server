package io.thoth.openapi.typescript.types

import io.thoth.openapi.common.ClassType
import java.math.BigDecimal
import java.math.BigInteger

class NumberTsGenerator : TsGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        return "number"
    }

    override fun parseMethod(classType: ClassType): ParseMethod = ParseMethod.TEXT

    override fun insertionMode(classType: ClassType) = InsertionMode.INLINE

    override fun generateName(classType: ClassType, generateSubType: GenerateType): String = "number"

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
