package io.thoth.openapi

import io.thoth.common.extensions.ClassType
import io.thoth.common.extensions.genericArguments
import io.thoth.common.extensions.parameterizedValues
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.typeOf
import kotlin.test.assertEquals
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
    fun testGenericMembersList() {
        val genericMembers = List::class.parameterizedValues
        assertEquals(0, genericMembers.size)
    }

    @Test
    fun testGenericMembersInner() {
        val genericMembers = InnerType::class.parameterizedValues
        assertEquals(0, genericMembers.size)
    }

    @Test
    fun testGenericMembersWrapper() {
        val genericMembers =
            ListWrapper::class
                .parameterizedValues
                .mapValues { entry -> entry.value.map { it.toString() } }
                .mapKeys { entry -> entry.key.name }
        assertEquals(mapOf("listProp1" to listOf("T"), "listProp2" to listOf("V")), genericMembers)
    }

    @Test
    fun testGenericMembersTwoListWrapper() {
        val genericMembers =
            TwoListWrapper::class
                .parameterizedValues
                .mapValues { entry -> entry.value.map { it.toString() } }
                .mapKeys { entry -> entry.key.name }
        assertEquals(mapOf("list" to listOf("V"), "listWrapper" to listOf("T", "V")), genericMembers)
    }

    @Test
    fun testClassType() {
        val type = ClassType.create<TwoListWrapper<InnerType, SecondInnerType>>()
        val listWrapper = type.fromMember(TwoListWrapper<*, *>::listWrapper)
        val parameterized =
            listWrapper.parameterizedValues.mapValues { entry -> listWrapper.fromMember(entry.key).clazz }
        assertEquals(
            mapOf<KProperty1<*, *>, KClass<*>>(
                ListWrapper<*, *>::listGeneric to List::class,
            ),
            parameterized,
        )

        val generic = listWrapper.resolvedGenericValues
        assertEquals(
            mapOf<KProperty1<*, *>, KClass<*>>(
                ListWrapper<*, *>::noListGeneric to InnerType::class,
            ),
            generic,
        )
    }
}
