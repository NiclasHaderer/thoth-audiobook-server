package io.github.huiibuh.db.tables

import io.github.huiibuh.db.ToModel
import io.github.huiibuh.models.AuthorModel
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.*

object TAuthors : UUIDTable("Authors") {
    val name = varchar("name", 255).uniqueIndex()
    val biography = text("biography").nullable()
    val providerID = reference("providerID", TProviderID).nullable()
    val image = reference("image", TImages).nullable()
}


class Author(id: EntityID<UUID>) : UUIDEntity(id), ToModel<AuthorModel> {
    companion object : UUIDEntityClass<Author>(TAuthors)

    private val imageID by TAuthors.image

    var name by TAuthors.name
    var biography by TAuthors.biography
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
