package io.thoth.database.tables

import io.thoth.database.tables.meta.MetaAuthor
import io.thoth.database.tables.meta.TMetaAuthors
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.*

object TAuthors : UUIDTable("Authors") {
    val name = varchar("name", 255).uniqueIndex()
    val biography = text("biography").nullable()
    val updateTime = datetime("updateTime").default(LocalDateTime.now())
    val image = reference("image", TImages, onDelete = ReferenceOption.CASCADE).nullable()
    val linkedTo = reference("linkedTo", TMetaAuthors, onDelete = ReferenceOption.CASCADE).nullable()
}


class Author(id: EntityID<UUID>) : UUIDEntity(id) {
    var name by TAuthors.name
    var biography by TAuthors.biography
    var updateTime by TAuthors.updateTime
    var imageId by TAuthors.image
    var linkedTo by MetaAuthor optionalReferencedOn TAuthors.linkedTo

    companion object : UUIDEntityClass<Author>(TAuthors)
}
