package io.thoth.server.common.exposed

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ExpressionWithColumnType
import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.alias


class Layered<T>(
    private val user: Column<T>,
    private val agent: Column<T>,
    private val file: Column<T>,
    private val preferFile: Column<Boolean>,
    override val columnType: IColumnType<T & Any>,
) : ExpressionWithColumnType<T>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder {
            append("COALESCE(")
            +user
            append(", CASE WHEN ")
            +preferFile
            append(" THEN COALESCE(")
            +file
            append(", ")
            +agent
            append(") ELSE COALESCE(")
            +agent
            append(", ")
            +file
            append(") END)")
        }
    }
}
/**
 * Resolves one metadata field across the three source layers: a user value always wins, and
 * [preferFile] decides whether the file tags or the metadata agent gets the next say.
 */
fun <L : Any, T> layered(
    user: L,
    agent: L,
    file: L,
    preferFile: Column<Boolean>,
    pick: L.() -> Column<T>,
) = Layered(user.pick(), agent.pick(), file.pick(), preferFile, user.pick().columnType)
    .alias(user.pick().name)
