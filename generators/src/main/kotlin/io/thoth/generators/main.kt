package io.thoth.generators
//
//import java.lang.reflect.ParameterizedType
//import kotlin.reflect.KProperty
//import kotlin.reflect.KType
//import kotlin.reflect.KTypeParameter
//import kotlin.reflect.KVisibility
//import kotlin.reflect.full.createType
//import kotlin.reflect.full.memberProperties
//
//open class Fuck<T>
//
//val KType.isGeneric get() = this.classifier is KTypeParameter
//
//data class HelloResponse(
//    val response: String
//)
//
//data class HelloRequest<T>(
//    val request: T,
//    val response: Any
//)
//
//inline fun <reified TYPE> asdf() {
//
//    val inlineRefined = object : Fuck<TYPE>() {}
//
//    val asdff = (inlineRefined.javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0]
//
//    val refinedClass =
//        ((inlineRefined.javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as ParameterizedType)
//
//    val clazz = refinedClass.rawType.let {
//        Class.forName((it as Class<*>).canonicalName).kotlin
//    }
//
//    val publicMembers = clazz.memberProperties.filter { it.visibility == KVisibility.PUBLIC }
//
//    val genericArgs = refinedClass.actualTypeArguments.map {
//        Class.forName((it as Class<*>).canonicalName).kotlin
//    }
//
//    val memberTypes = publicMembers.map { (it as KProperty<*>).returnType }
//    val asdf = memberTypes.map {
//        if (it.isGeneric) {
//            val name = (it.classifier as KTypeParameter).createType()
//            println(name)
//        }
//    }
//    println(asdf)
//
//    println(genericArgs)
//
//    //    val extractedType = object : com.google.inject.TypeLiteral<TYPE>() {}
//    //
//    //    println(extractedType.type.typeName)
//    //    when (extractedType.type) {
//    //        is MoreTypes.ParameterizedTypeImpl -> println("param")
//    //        is MoreTypes.WildcardTypeImpl -> println("wildcard")
//    //        is MoreTypes.GenericArrayTypeImpl -> println("array")
//    //        else -> println("class")
//    //    }
//
//    //    val arg = (extractedType.type as MoreTypes.ParameterizedTypeImpl).actualTypeArguments[0] as Class<*>
//    //    val refClass = Class.forName(extractedType.rawType.canonicalName).kotlin.memberProperties
//    //    val args = Class.forName(arg.canonicalName).kotlin
//    //    println(args)
//    //    println(refClass)
//}
//
//fun main() {
//    asdf<HelloRequest<HelloResponse>>()
//    asdf<List<String>>()
//    asdf<HelloResponse>()
//    asdf<HelloRequest<*>>()
//}
