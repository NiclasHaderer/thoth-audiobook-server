package io.thoth.database.tables

import io.thoth.common.extensions.findOne
import io.thoth.database.ToModel
import io.thoth.models.AuthorModel
import io.thoth.models.AuthorModelWithBooks
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.*

object TAuthors : UUIDTable("Authors") {
    val name = varchar("name", 255).uniqueIndex()
    val biography = text("biography").nullable()
    val updateTime = datetime("updateTime").default(LocalDateTime.now())
    val providerID = reference("providerID", TProviderID).nullable()
    val image = reference("image", TImages).nullable()
}


class Author(id: EntityID<UUID>) : UUIDEntity(id), ToModel<AuthorModel> {
    companion object : UUIDEntityClass<Author>(TAuthors) {
        fun removeUnused() = transaction {
            all().forEach {
                if (Book.find { TBooks.author eq it.id.value }.empty()) {
                    it.delete()
                }
            }
        }

        fun getById(uuid: UUID, order: SortOrder = SortOrder.ASC) = transaction {
            val author = Author.findById(uuid)?.toModel() ?: return@transaction null
            val books = Book.fromAuthor(uuid, order)
            val index = Author.all().orderBy(TAuthors.name.lowerCase() to order).indexOfFirst { it.id.value == uuid }
            AuthorModelWithBooks.fromModel(author, books, index)
        }

        fun getMultiple(limit: Int, offset: Long, order: SortOrder = SortOrder.ASC) = transaction {
            Author.all().limit(limit, offset * limit).orderBy(TAuthors.name.lowerCase() to order).map { it.toModel() }
        }

        fun totalCount() = transaction { Author.all().count() }

        fun getByName(authorName: String): Author? = transaction {
            Author.findOne { TAuthors.name like authorName }
        }

        fun exists(authorName: String): Boolean = Author.findOne { TAuthors.name like authorName } != null
    }

    private val imageID by TAuthors.image

    var name by TAuthors.name
    var biography by TAuthors.biography
    var updateTime by TAuthors.updateTime
    var providerID by ProviderID optionalReferencedOn TAuthors.providerID
    var image by Image optionalReferencedOn TAuthors.image

    override fun toModel() = AuthorModel(
        id = id.value,
        name = name,
        biography = biography,
        providerID = providerID?.toModel(),
        image = imageID?.value
    )
}
