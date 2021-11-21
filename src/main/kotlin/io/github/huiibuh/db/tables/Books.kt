package io.github.huiibuh.db.tables

import io.github.huiibuh.models.BookModel
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*


object TBooks : UUIDTable("books") {
    val title = varchar("title", 255)
    val language = varchar("language", 255).nullable()
    val description = text("description").nullable()
    val author = reference("author", TAuthors)
    val asin = char("asin", 10).uniqueIndex().nullable()
    val narrator = reference("narrator", TAuthors).nullable()
    val series = reference("series", TSeries).nullable()
    val seriesIndex = integer("seriesIndex").nullable()
    val cover = reference("cover", TImages).nullable()
}

class Book(id: EntityID<UUID>) : UUIDEntity(id), ToModel<BookModel> {
    companion object : UUIDEntityClass<Book>(TBooks)

    private val authorID by TBooks.author
    private val narratorID by TBooks.narrator
    private val seriesID by TBooks.series
    private val coverID by TBooks.cover

    var title by TBooks.title
    var language by TBooks.language
    val description by TBooks.description
    val asin by TBooks.asin
    var author by Author referencedOn TBooks.author
    var narrator by Author optionalReferencedOn TBooks.narrator
    var series by Series optionalReferencedOn TBooks.series
    var seriesIndex by TBooks.seriesIndex
    var cover by Image optionalReferencedOn TBooks.cover

    override fun toModel() = BookModel(
        id = id.value,
        title = title,
        language = language,
        description = description,
        asin = asin,
        author = authorID.value,
        narrator = narratorID?.value,
        series = seriesID?.value,
        seriesIndex = seriesIndex,
        cover = coverID?.value
    )
}
