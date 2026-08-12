package io.thoth.server.common.extensions

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ComplexExpression
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.LikePattern
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.append
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.stringParam

class ILikeOp<T : String?>(
    private val expr: Expression<T>,
    private val pattern: LikePattern,
) : Op<Boolean>(),
    ComplexExpression {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        with(queryBuilder) {
            val param = stringParam(pattern.pattern)
            append(expr.lowerCase(), " LIKE ", param.lowerCase())
            pattern.escapeChar?.let { append(" ESCAPE ", stringParam(it.toString())) }
        }
    }
}

class JsonEach(
    private val list: Column<List<String>?>,
) : Table("item") {
    val value = text("value")

    override fun describe(
        s: Transaction,
        queryBuilder: QueryBuilder,
    ) {
        queryBuilder { append("json_each(", list, ") AS ", tableName) }
    }
}

fun Column<List<String>?>.jsonEach(): JsonEach = JsonEach(this)

private const val LIKE_ESCAPE_CHAR = '\\'

infix fun <T : String?> Expression<T>.ilike(pattern: LikePattern): Op<Boolean> = ILikeOp(this, pattern)

infix fun <T : String?> Expression<T>.ilike(pattern: String): Op<Boolean> =
    ilike(LikePattern(pattern, LIKE_ESCAPE_CHAR))

fun escape(value: String): String = LikePattern.ofLiteral(value, LIKE_ESCAPE_CHAR).pattern
