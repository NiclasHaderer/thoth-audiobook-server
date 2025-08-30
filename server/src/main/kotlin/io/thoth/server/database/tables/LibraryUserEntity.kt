package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.CompositeEntity
import org.jetbrains.exposed.v1.dao.CompositeEntityClass

class LibraryUserEntity(
    id: EntityID<CompositeID>,
) : CompositeEntity(id) {
    companion object : CompositeEntityClass<LibraryUserEntity>(LibraryUserTable)

    var permissions by LibraryUserTable.permissions
    var library by LibraryEntity referencedOn LibraryUserTable.library
    var user by UserEntity referencedOn LibraryUserTable.user
}
