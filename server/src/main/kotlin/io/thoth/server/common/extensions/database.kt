package io.thoth.server.common.extensions

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction

fun <E : UUIDEntity> UUIDEntityClass<E>.findOne(op: SqlExpressionBuilder.() -> Op<Boolean>): E? {
    return this.find(op).limit(1).firstOrNull()
}

fun Transaction.addMissingColumns(vararg tables: Table) {
    val statements = SchemaUtils.addMissingColumnsStatements(*tables)
    if (statements.isNotEmpty()) {
        execInBatch(statements)
    }
}

fun <T> List<T>.toSizedIterable(): SizedIterable<T> {
    return SizedCollection<T>(this)
}

fun <T : UUIDEntity> SizedIterable<T>.add(newEntry: T?): SizedIterable<T> {
    if (newEntry == null) {
        return this
    }
    val newCollection = this.toMutableList()
    newCollection.add(newEntry)
    return SizedCollection(newCollection)
}
