package io.github.huiibuh.db.models

import org.jetbrains.exposed.sql.Table

// TODO artist foreign key
object Artist : Table() {
    val id = integer("id").autoIncrement()
    override val primaryKey = PrimaryKey(id)
    val name = varchar("name", 255)
    val description = text("description")
    val asin = text("asin")
    val image = binary("image")
}

