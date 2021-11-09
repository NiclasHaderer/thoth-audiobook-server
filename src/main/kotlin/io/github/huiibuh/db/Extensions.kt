package io.github.huiibuh.db

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder


fun <E : IntEntity> IntEntityClass<E>.findOne(op: SqlExpressionBuilder.() -> Op<Boolean>): E? {
    val res = this.find(op)
    if (res.empty()) return null
    return res.first()
}
