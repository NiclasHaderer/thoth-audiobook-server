package io.github.huiibuh.db.tables

import io.github.huiibuh.api.exceptions.APINotFound
import io.github.huiibuh.db.ToModel
import io.github.huiibuh.db.update.interceptor.TimeUpdatable
import io.github.huiibuh.extensions.findOne
import io.github.huiibuh.models.AuthorModel
import io.github.huiibuh.models.AuthorModelWithBooks
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


class Author(id: EntityID<UUID>) : UUIDEntity(id), ToModel<AuthorModel>, TimeUpdatable {
    companion object : UUIDEntityClass<Author>(TAuthors) {
        fun removeUnused() = transaction {
            all().forEach {
                if (Book.find { TBooks.author eq it.id.value }.empty()) {
                    it.delete()
                }
            }
        }

        @Throws(APINotFound::class)
        fun getById(uuid: UUID, order: SortOrder = SortOrder.ASC) = transaction {
            val author = Author.findById(uuid)?.toModel() ?: throw APINotFound("Could not find author")
            val books =
                Book.find { TBooks.author eq uuid }.orderBy(TBooks.title.lowerCase() to order).map { it.toModel() }
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
    }

    private val imageID by TAuthors.image

    var name by TAuthors.name
    var biography by TAuthors.biography
    override var updateTime by TAuthors.updateTime
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
