package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class AuthorEntity(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<AuthorEntity>(AuthorTable)

    var name by AuthorTable.name
    var displayName by AuthorTable.displayName
    var biography by AuthorTable.biography
    var imageID by AuthorTable.imageID
    var website by AuthorTable.website
    var birthDate by AuthorTable.birthDate
    var bornIn by AuthorTable.bornIn
    var deathDate by AuthorTable.deathDate

    // Provider
    var provider by AuthorTable.provider
    var providerID by AuthorTable.providerID

    // Relations
    var books by BookEntity via AuthorBookTable
    var series by SeriesEntity via SeriesAuthorTable
    var library by LibraryEntity referencedOn AuthorTable.library
}
