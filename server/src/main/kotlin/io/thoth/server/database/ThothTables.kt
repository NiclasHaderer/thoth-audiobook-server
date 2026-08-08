package io.thoth.server.database

import io.github.classgraph.ClassGraph
import io.thoth.server.common.exposed.View
import org.jetbrains.exposed.v1.core.Table

private const val TABLES_PACKAGE = "io.thoth.server.database.tables"

val THOTH_TABLES: Array<Table> by lazy {
    val tables =
        ClassGraph()
            .acceptPackages(TABLES_PACKAGE)
            .enableClassInfo()
            .scan()
            .use { it.allClasses.loadClasses() }
            .mapNotNull { it.kotlin.objectInstance as? Table }
            .filterNot { it is View }
            .sortedBy { it.tableName }

    check(tables.isNotEmpty()) { "No Exposed tables found in $TABLES_PACKAGE" }
    tables.toTypedArray()
}
