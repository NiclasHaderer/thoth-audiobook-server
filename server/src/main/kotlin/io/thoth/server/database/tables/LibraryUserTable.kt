package io.thoth.server.database.tables

import io.thoth.models.LibraryPermissions
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable

object LibraryUserTable : CompositeIdTable("LibraryUser") {
    val library = reference("library", LibrariesTable, onDelete = ReferenceOption.CASCADE)
    val user = reference("user", UsersTable, onDelete = ReferenceOption.CASCADE)
    var permissions = enumeration<LibraryPermissions>("permissions")
    override val primaryKey = PrimaryKey(library, user)

    init {
        addIdColumn(user)
        addIdColumn(library)
    }
}
