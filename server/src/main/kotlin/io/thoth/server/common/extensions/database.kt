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
