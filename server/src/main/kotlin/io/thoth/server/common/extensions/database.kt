package io.thoth.server.common.extensions

import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.SizedCollection
import org.jetbrains.exposed.v1.jdbc.SizedIterable

fun <E : UUIDEntity> UUIDEntityClass<E>.findOne(op: () -> Op<Boolean>): E? =
    this.find(op).limit(1).firstOrNull()

fun <T> List<T>.toSizedIterable(): SizedIterable<T> = SizedCollection<T>(this)

fun <T : UUIDEntity> SizedIterable<T>.add(newEntry: T?): SizedIterable<T> {
    if (newEntry == null) return this
    val newCollection = this.toMutableList()
    newCollection.add(newEntry)
    return SizedCollection(newCollection)
}

fun <T : UUIDEntity> SizedIterable<T>.add(newEntry: List<T>?): SizedIterable<T> {
    if (newEntry == null) return this
    val newCollection = this.toMutableList()
    newCollection.addAll(newEntry)
    return SizedCollection(newCollection)
}
