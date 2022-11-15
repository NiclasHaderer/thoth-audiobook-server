package io.thoth.common.extensions

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder


fun <E : UUIDEntity> UUIDEntityClass<E>.findOne(op: SqlExpressionBuilder.() -> Op<Boolean>): E? {
    return this.find(op).limit(1).firstOrNull()
}

