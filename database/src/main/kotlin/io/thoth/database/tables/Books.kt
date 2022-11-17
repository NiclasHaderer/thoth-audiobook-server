package io.thoth.database.tables

import io.thoth.database.tables.meta.MetaBook
import io.thoth.database.tables.meta.TMetaBooks
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.*


object TBooks : UUIDTable("Books") {
    val title = varchar("title", 255)
    val author = reference("author", TAuthors)
    val date = date("year").nullable()
    val updateTime = datetime("updateTime").default(LocalDateTime.now())
    val language = varchar("language", 255).nullable()
    val description = text("description").nullable()
    val narrator = varchar("name", 255).nullable()
    val series = reference("series", TSeries).nullable()
    val seriesIndex = float("seriesIndex").nullable()
    val cover = reference("cover", TImages).nullable()
    val linkedTo = reference("linkedTo", TMetaBooks, onDelete = ReferenceOption.CASCADE).nullable()
}

class Book(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Book>(TBooks)

    var title by TBooks.title
    var date by TBooks.date
    var language by TBooks.language
    var description by TBooks.description
    var updateTime by TBooks.updateTime
    var author by Author referencedOn TBooks.author
    var narrator by TBooks.narrator
    var series by Series optionalReferencedOn TBooks.series
    var seriesIndex by TBooks.seriesIndex
    var cover by TBooks.cover
    var linkedTo by MetaBook optionalReferencedOn TBooks.linkedTo
}
