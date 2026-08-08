package io.thoth.server.database

import io.github.classgraph.ClassGraph
import io.thoth.server.common.exposed.View

private const val VIEWS_PACKAGE = "io.thoth.server.database.views"

val THOTH_VIEWS: List<View> by lazy {
    ClassGraph()
        .acceptPackages(VIEWS_PACKAGE)
        .enableClassInfo()
        .scan()
        .use { it.allClasses.loadClasses() }
        .mapNotNull { it.kotlin.objectInstance as? View }
        .sortedBy { it.tableName }
}
