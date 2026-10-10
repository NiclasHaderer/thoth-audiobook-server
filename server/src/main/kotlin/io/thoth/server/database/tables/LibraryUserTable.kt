package io.thoth.server.database.tables

import io.thoth.models.LibraryPermissionLevel
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable

object LibraryUserTable : CompositeIdTable("library_user") {
    val library = reference("library_id", LibraryTable, onDelete = ReferenceOption.CASCADE)
    val user = reference("user_id", UserTable, onDelete = ReferenceOption.CASCADE).index()
    val permissions = enumerationByName<LibraryPermissionLevel>("permissions", 16)
    override val primaryKey = PrimaryKey(library, user)

    init {
        addIdColumn(user)
        addIdColumn(library)
    }
}
