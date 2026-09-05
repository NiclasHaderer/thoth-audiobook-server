package io.thoth.server.common.exposed

import org.jetbrains.exposed.v1.core.AbstractQuery
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.IExpressionAlias
import org.jetbrains.exposed.v1.core.InternalApi
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.transactions.currentTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

abstract class View(
    name: String,
) : Table(name) {
    protected abstract fun body(): AbstractQuery<*>

    internal fun dependencies(): List<View> = body().targets.filterIsInstance<View>()

    @OptIn(InternalApi::class)
    override fun createStatement(): List<String> {
        val tx = currentTransaction()
        val query = body()

        check(columns.isNotEmpty()) { "View $tableName declares no columns" }
        check(query.set.realFields.size == columns.size) {
            "View $tableName declares ${columns.size} columns but its body selects ${query.set.realFields.size}"
        }
        // The column list binds positionally
        columns.zip(query.set.realFields).forEach { (column, field) ->
            val selected =
                when (field) {
                    is Column<*> -> field.name

                    is IExpressionAlias<*> -> field.alias

                    else -> error(
                        "View $tableName selects an unnamed expression for column '${column.name}'. " +
                            "Add .alias(\"${column.name}\") to it",
                    )
                }
            check(selected == column.name) {
                "View $tableName declares column '${column.name}' where its body selects '$selected'. " +
                    "Columns must be declared in select order"
            }
        }

        val builder = QueryBuilder(prepared = false)
        val select = query.prepareSQL(builder)

        val columnList = columns.joinToString { tx.identity(it) }
        return listOf("CREATE VIEW ${tx.identity(this)} ($columnList) AS $select")
    }

    @OptIn(InternalApi::class)
    override fun dropStatement(): List<String> = listOf("DROP VIEW IF EXISTS ${currentTransaction().identity(this)}")

    override fun modifyStatement(): List<String> =
        throw UnsupportedOperationException("Views are dropped and recreated, not modified")
}

fun syncViews(views: List<View>) {
    if (views.isEmpty()) return
    transaction {
        val ordered = views.inDependencyOrder()
        ordered.asReversed().forEach { exec(it.dropStatement().single()) }
        ordered.forEach { exec(it.createStatement().single()) }
    }
}

private fun List<View>.inDependencyOrder(): List<View> {
    val ordered = mutableListOf<View>()
    val onPath = mutableSetOf<View>()

    fun visit(view: View) {
        if (view in ordered) return
        check(onPath.add(view)) { "View dependency cycle involving ${view.tableName}" }
        view.dependencies().forEach(::visit)
        onPath.remove(view)
        ordered += view
    }

    forEach(::visit)
    return ordered
}
