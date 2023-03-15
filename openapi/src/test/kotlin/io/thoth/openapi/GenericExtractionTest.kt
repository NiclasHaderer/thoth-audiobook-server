package io.thoth.openapi

import io.thoth.openapi.schema.TypeToken2
import io.thoth.openapi.schema.genericMembers
import io.thoth.openapi.schema.getGenericTypes
import kotlin.test.assertEquals
import org.junit.Test

class InnerType

class SecondInnerType

data class ListWrapper<T, V>(val listProp1: List<T>, val listProp2: List<V>)

data class TwoListWrapper<T, V>(val listWrapper: ListWrapper<T, V>, val list: List<V>, val notGeneric: String)

class GenericExtractionTest {
    @Test
    fun testSimpleList() {
        val type = object : TypeToken2<List<InnerType>>() {}.type
        val resolvedType = getGenericTypes(type)
        assertEquals(listOf(InnerType::class), resolvedType)
    }

    @Test
    fun testNestedList() {
        val type = object : TypeToken2<ListWrapper<InnerType, SecondInnerType>>() {}.type
        val resolvedType = getGenericTypes(type)
        assertEquals(listOf(InnerType::class, SecondInnerType::class), resolvedType)
    }

    @Test
    fun testNestedListAndNormalList() {
        val type = object : TypeToken2<TwoListWrapper<SecondInnerType, InnerType>>() {}.type
        val resolvedType = getGenericTypes(type)
        assertEquals(listOf(SecondInnerType::class, InnerType::class), resolvedType)
    }

    @Test
    fun testGenericMembersList() {
        val genericMembers = List::class.genericMembers
        assertEquals(0, genericMembers.size)
    }

    @Test
    fun testGenericMembersInner() {
        val genericMembers = InnerType::class.genericMembers
        assertEquals(0, genericMembers.size)
    }

    @Test
    fun testGenericMembersWrapper() {
        val genericMembers = ListWrapper::class.genericMembers.mapValues { entry -> entry.value.map { it.toString() } }
        assertEquals(mapOf("listProp1" to listOf("T"), "listProp2" to listOf("V")), genericMembers)
    }

    @Test
    fun testGenericMembersTwoListWrapper() {
        val genericMembers =
            TwoListWrapper::class.genericMembers.mapValues { entry -> entry.value.map { it.toString() } }
        assertEquals(mapOf("list" to listOf("V"), "listWrapper" to listOf("T", "V")), genericMembers)
    }
}
