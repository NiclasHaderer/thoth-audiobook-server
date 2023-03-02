package io.thoth.common.extensions

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.sql.*

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
