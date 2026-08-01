package io.thoth.server.common.extensions

import org.jetbrains.exposed.v1.core.ComplexExpression
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.LikePattern
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.append
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.stringParam
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.core.vendors.currentDialect
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.SizedCollection
import org.jetbrains.exposed.v1.jdbc.SizedIterable

fun <E : UUIDEntity> UUIDEntityClass<E>.findOne(op: () -> Op<Boolean>): E? =
    this.find(op).limit(1).firstOrNull()

class ILikeOp<T : String?>(
    private val expr: Expression<T>,
    private val pattern: LikePattern,
) : Op<Boolean>(),
    ComplexExpression {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        with(queryBuilder) {
            val param = stringParam(pattern.pattern)
            if (currentDialect is PostgreSQLDialect) {
                append(expr, " ILIKE ", param)
            } else {
                append(expr.lowerCase(), " LIKE ", param.lowerCase())
            }
            pattern.escapeChar?.let { append(" ESCAPE ", stringParam(it.toString())) }
        }
    }
}

private const val LIKE_ESCAPE_CHAR = '\\'

infix fun <T : String?> Expression<T>.ilike(pattern: LikePattern): Op<Boolean> = ILikeOp(this, pattern)

infix fun <T : String?> Expression<T>.ilike(pattern: String): Op<Boolean> =
    ilike(LikePattern(pattern, LIKE_ESCAPE_CHAR))

fun escape(value: String): String = LikePattern.ofLiteral(value, LIKE_ESCAPE_CHAR).pattern

fun <T> List<T>.toSizedIterable(): SizedIterable<T> = SizedCollection<T>(this)

fun <T : UUIDEntity> SizedIterable<T>.add(newEntry: T?): SizedIterable<T> {
    if (newEntry == null) return this
    val newCollection = this.toMutableList()
    newCollection.add(newEntry)
    return SizedCollection(newCollection)
}
