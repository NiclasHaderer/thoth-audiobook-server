package io.thoth.openapi

import io.thoth.openapi.schema.ClassType
import kotlin.test.expect
import org.junit.Test

open class Animal {
    val name: String = ""
    val age: Int = 0

    open val type: String
        get() = "Animal"
}

class Dog : Animal() {
    val breed: String = "Australian Shepherd"

    override val type: String
        get() = "Dog"
}

class ClassTypeTest {
    @Test
    fun testMemberProperties() {
        val classType = ClassType.create<Animal>()
        val properties = classType.memberProperties.toList()
        expect(2) { properties.size }
        expect("name") { properties[0].name }
        expect("age") { properties[1].name }
    }
}
