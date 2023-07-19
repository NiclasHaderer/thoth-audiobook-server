package io.thoth.openapi.typescript.types

import io.thoth.openapi.common.ClassType

typealias GenerateType = (classType: ClassType) -> TsGenerator.Type

abstract class TsGenerator {
    class Type
    internal constructor(
        val name: String,
        val content: String,
        val inlineMode: InsertionMode,
        val parser: ParseMethod
    ) {
        fun reference(): String = if (inlineMode == InsertionMode.INLINE) content else name

        override fun toString(): String = content
    }

    enum class ParseMethod(val methodName: String) {
        BLOB("blob"),
        JSON("json"),
        TEXT("text"),
    }

    enum class InsertionMode {
        INLINE,
        REFERENCE,
    }

    abstract fun generateContent(classType: ClassType, generateSubType: (classType: ClassType) -> Type): String

    abstract fun parseMethod(classType: ClassType): ParseMethod

    fun createType(classType: ClassType, generateSubType: GenerateType): Type {
        return Type(
            name = generateName(classType, generateSubType),
            content = generateContent(classType, generateSubType),
            inlineMode = insertionMode(classType),
            parser = parseMethod(classType),
        )
    }

    abstract fun insertionMode(classType: ClassType): InsertionMode

    abstract fun generateName(classType: ClassType, generateSubType: GenerateType): String

    abstract fun canGenerate(classType: ClassType): Boolean

    open fun priority(classType: ClassType): Int = 0
}

val tsGenerators: List<TsGenerator> =
    listOf(
        ArrayTsGenerator(),
        BinaryTsGenerator(),
        BooleanTsGenerator(),
        ByteArrayTsGenerator(),
        DateTsGenerator(),
        EnumTsGenerator(),
        InterfaceTsGenerator(),
        NumberTsGenerator(),
        PairTsGenerator(),
        RecordTsGenerator(),
        RedirectTsGenerator(),
        StringTsGenerator(),
        UUIDTsGenerator(),
        UnitTsGenerator()
    )

fun generateTypes(classType: ClassType): Pair<TsGenerator.Type, List<TsGenerator.Type>> {
    val generator = tsGenerators.filter { it.canGenerate(classType) }.maxBy { it.priority(classType) }
    val generatedSubTypes = mutableListOf<TsGenerator.Type>()
    val type =
        generator.createType(classType) { subType ->
            val (actual, generatedSubType) = generateTypes(subType)
            generatedSubTypes.addAll(generatedSubType)
            actual
        }
    generatedSubTypes.add(type)
    return type to generatedSubTypes
}
