package io.thoth.openapi

import io.thoth.openapi.common.ClassType
import java.math.BigInteger
import kotlin.test.expect
import org.junit.Test

class InnerType

class SecondInnerType

data class ListWrapper<D, E>(val noListGeneric: D, val listGeneric: List<E>)

data class TwoListWrapper<A, B, C>(
    val listWrapper: ListWrapper<A, B>,
    val list: List<B>,
    val simpleGeneric: C,
    val notGeneric: String,
)

class Outer<H>(val a: Map<BigInteger, H>) {
    class InnerTwo<F, G>(val b: Map<F, G>)
}

abstract class SomeInterface<I>(val i: I)

class FinalType : SomeInterface<String>("")

class GenericExtractionTest {

    @Test
    fun testClassType() {
        val type = ClassType.create<TwoListWrapper<InnerType, SecondInnerType, String>>()
        val listWrapper = type.forMember(TwoListWrapper<*, *, *>::listWrapper)

        expect(ListWrapper::class) { listWrapper.clazz }
        expect(InnerType::class) { listWrapper.forMember(ListWrapper<*, *>::noListGeneric).clazz }
        expect(SecondInnerType::class) {
            listWrapper.forMember(ListWrapper<*, *>::listGeneric).genericArguments[0].clazz
        }

        val list = type.forMember(TwoListWrapper<*, *, *>::list)
        expect(List::class) { list.clazz }
        expect(SecondInnerType::class) { list.genericArguments[0].clazz }

        // Test that we can get the type of simple generic members
        val simpleGeneric = type.forMember(TwoListWrapper<*, *, *>::simpleGeneric)
        expect(String::class) { simpleGeneric.clazz }

        // Test that we can get the type of non-generic members
        val notGeneric = type.forMember(TwoListWrapper<*, *, *>::notGeneric)
        expect(String::class) { notGeneric.clazz }
    }

    @Test
    fun testSomething() {
        data class HardToResolve<LONG_TYPE_PARAM>(val a: LONG_TYPE_PARAM, val b: List<LONG_TYPE_PARAM>, val c: String)
        class Unique

        val classType = ClassType.create<HardToResolve<List<Unique>>>()

        val aMember = classType.forMember(HardToResolve<*>::a)
        expect(aMember.clazz) { List::class }
        expect(aMember.genericArguments.map { it.clazz }) { listOf(Unique::class) }

        val bMember = classType.forMember(HardToResolve<*>::b)
        expect(bMember.clazz) { List::class }
        bMember.genericArguments.forEach { member ->
            expect(member.clazz) { List::class }
            member.genericArguments.forEach { expect(it.clazz) { Unique::class } }
        }

        val cMember = classType.forMember(HardToResolve<*>::c)
        expect(cMember.clazz) { String::class }
    }

    @Test
    fun testMissingParametersAndInlineGenerics() {
        class TestClass<T>(val b: Map<String, T>)

        val classType = ClassType.create<TestClass<Int>>()

        val bMember = classType.forMember(TestClass<*>::b)
        expect(Map::class) { bMember.clazz }
        expect(listOf(String::class, Int::class)) { bMember.genericArguments.map { it.clazz } }
    }

    @Test
    fun testInnerClass() {
        val classType = ClassType.create<Outer.InnerTwo<String, Int>>()

        val bMember = classType.forMember(Outer.InnerTwo<*, *>::b)
        expect(Map::class) { bMember.clazz }
        expect(String::class) { bMember.genericArguments[0].clazz }
        expect(Int::class) { bMember.genericArguments[1].clazz }

        val parent = classType.parent!!
        expect(Outer::class) { parent.clazz }

        val aMember = parent.forMember(Outer<*>::a)
        expect(Map::class) { aMember.clazz }
        expect(BigInteger::class) { aMember.genericArguments[0].clazz }
        expect(Any::class) { aMember.genericArguments[1].clazz }
    }

    @Test
    fun testsSuperClasses() {
        val classType = ClassType.create<FinalType>()
        val superClasses = classType.superClasses
        expect(1) { superClasses.size }
        expect(SomeInterface::class) { superClasses[0].clazz }
        expect(Any::class) { superClasses[0].genericArguments[0].clazz }
    }
}
