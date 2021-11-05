package io.github.huiibuh.db.models

import org.jetbrains.exposed.sql.Table


// TODO album foreign key
object Track : Table() {
    val id = integer("id").autoIncrement()
    override val primaryKey = PrimaryKey(id)

    val title = varchar("title", 255)
    val duration = float("duration")
    val track = integer("track")
    val album = varchar("album", 255)
    val artist = varchar("artist", 255)
    val composer = varchar("composer", 255)
    val collection = varchar("collection", 255)
    val collectionIndex = integer("collectionIndex")
    val cover = binary("cover")
}
