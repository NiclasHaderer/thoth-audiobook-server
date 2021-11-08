package io.github.huiibuh.db.migration

import org.jetbrains.exposed.sql.Database
import org.reflections.Reflections
import org.slf4j.LoggerFactory
import kotlin.reflect.full.createInstance

class DatabaseMigrator(val db: Database, val packageName: String) {
    private val log = LoggerFactory.getLogger(this::class.java)

    private val classes: List<Migration> by lazy {
        val reflections = Reflections(this.packageName)
        val classes = reflections.getSubTypesOf(Migration::class.java)
        classes.map { it.kotlin }.map { it.createInstance() }
    }

    private val sortedClasses: List<Migration> by lazy {
        classes.sortedBy { migration -> migration::class.java.simpleName }
    }

    fun runMigrations() {
        sortedClasses.forEach {
            try {
                it.migrate(db)
            } catch (e: Exception) {
                it.rollback(db)
                throw e
            }
        }
    }

}
