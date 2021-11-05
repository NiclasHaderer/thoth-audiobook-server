package io.github.huiibuh.db.models

import org.jetbrains.exposed.sql.Table


// TODO tracks foreign key
// TODO author foreign key
object Album : Table() {
    val id = integer("id").autoIncrement()
    override val primaryKey = PrimaryKey(id)

    val name = varchar("name", 255)
    val composer = varchar("composer", 255)
    val artist = varchar("artist", 255)
    val collectionIndex = integer("collectionIndex")
    val collection = varchar("collection", 255)
    val cover = binary("cover")
}
