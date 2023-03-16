package io.thoth.openapi

import io.thoth.common.extensions.genericArguments
import io.thoth.openapi.schema.ClassType
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.typeOf
import kotlin.test.assertEquals
import kotlin.test.expect
import org.junit.Test

class InnerType

class SecondInnerType

data class ListWrapper<T, V>(val noListGeneric: T, val listGeneric: List<V>)

data class TwoListWrapper<T, V>(val listWrapper: ListWrapper<T, V>, val list: List<V>, val notGeneric: String)

class GenericExtractionTest {
    @Test
    fun testSimpleList() {
        val type = typeOf<List<InnerType>>()
        val resolvedType = type.genericArguments
        assertEquals(listOf(InnerType::class), resolvedType)
    }

    @Test
    fun testNestedList() {
        val type = typeOf<ListWrapper<InnerType, SecondInnerType>>()
        val resolvedType = type.genericArguments
        assertEquals(listOf(InnerType::class, SecondInnerType::class), resolvedType)
    }

    @Test
    fun testNestedListAndNormalList() {
        val type = typeOf<TwoListWrapper<SecondInnerType, InnerType>>()
        val resolvedType = type.genericArguments
        assertEquals(listOf(SecondInnerType::class, InnerType::class), resolvedType)
    }

    @Test
    fun testClassType() {
        val type = ClassType.create<TwoListWrapper<InnerType, SecondInnerType>>()
        val listWrapper = type.fromMember(TwoListWrapper<*, *>::listWrapper)
        val parameterizedOuter = listWrapper.resolvedParameterizedValue.mapValues { it.value.clazz }
        assertEquals(
            mapOf<KProperty1<*, *>, KClass<*>>(
                ListWrapper<*, *>::listGeneric to List::class,
            ),
            parameterizedOuter,
        )

        val generic = listWrapper.resolvedGenericValues.mapValues { it.value.clazz }
        assertEquals(
            mapOf<KProperty1<*, *>, KClass<*>>(
                ListWrapper<*, *>::noListGeneric to InnerType::class,
            ),
            generic,
        )

        val parameterizedInner =
            listWrapper.resolvedParameterizedValue.mapValues {
                Pair(
                    it.value.clazz,
                    it.value.genericArguments.map { it.clazz },
                )
            }
        assertEquals(
            mapOf<KProperty1<*, *>, Pair<KClass<*>, List<KClass<*>>>>(
                ListWrapper<*, *>::listGeneric to Pair(List::class, listOf(SecondInnerType::class)),
            ),
            parameterizedInner,
        )
    }

    @Test
    fun testSomething() {
        data class HardToResolve<LONG_TYPE_PARAM>(val a: LONG_TYPE_PARAM, val b: List<LONG_TYPE_PARAM>, val c: String)
        class Unique

        val classType = ClassType.create<HardToResolve<List<Unique>>>()

        val aMember = classType.fromMember(HardToResolve<*>::a)
        expect(aMember.clazz) { List::class }
        expect(aMember.genericArguments.map { it.clazz }) { listOf(Unique::class) }

        val bMember = classType.fromMember(HardToResolve<*>::b)
        expect(bMember.clazz) { List::class }
        bMember.genericArguments.forEach {
            expect(it.clazz) { List::class }
            it.genericArguments.forEach { expect(it.clazz) { Unique::class } }
        }

        val cMember = classType.fromMember(HardToResolve<*>::c)
        expect(cMember.clazz) { String::class }
    }
}
