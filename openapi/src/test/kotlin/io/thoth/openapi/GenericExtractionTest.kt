package io.thoth.openapi

import com.google.gson.reflect.TypeToken
import io.thoth.openapi.schema.genericMembers
import io.thoth.openapi.schema.getGenericTypes
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import org.junit.Test

class InnerType

class SecondInnerType

class ListWrapper<T, V>(val listProp1: List<T>, val listProp2: List<V>)

class TwoListWrapper<T, V>(val listWrapper: ListWrapper<T, V>, val list: List<V>, val notGeneric: String)

inline fun <reified T> typeOf(): KClass<*> {
    return T::class
}

class GenericExtractionTest {
    @Test
    fun testSimpleList() {
        val type = object : TypeToken<List<InnerType>>() {}.type
        val clazz = typeOf<List<InnerType>>()
        val resolvedType = getGenericTypes(type)
        assertEquals(resolvedType as Any, InnerType::class.java)
    }

    @Test
    fun testNestedList() {
        val type = object : TypeToken<ListWrapper<InnerType, SecondInnerType>>() {}.type
        val resolvedType = getGenericTypes(type)
        assertEquals(resolvedType as Any, InnerType::class.java)
    }

    @Test
    fun testNestedListAndNormalList() {
        val clazz = TwoListWrapper::class.genericMembers
        print(clazz)

        val type = object : TypeToken<TwoListWrapper<SecondInnerType, InnerType>>() {}.type
        val resolvedType = getGenericTypes(type)
        assertEquals(resolvedType as Any, InnerType::class.java)
    }
}
