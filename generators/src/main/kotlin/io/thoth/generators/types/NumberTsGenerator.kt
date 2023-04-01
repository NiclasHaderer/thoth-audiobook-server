package io.thoth.generators.types

import io.thoth.openapi.schema.ClassType
import java.math.BigDecimal
import java.math.BigInteger

class NumberTsGenerator : TsGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        return "number"
    }

    override fun shouldInline(classType: ClassType): Boolean = true

    override fun generateName(classType: ClassType): String = "number"

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
        )
    }
}
