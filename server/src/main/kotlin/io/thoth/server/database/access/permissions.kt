package io.thoth.server.database.access

import io.thoth.models.LibraryPermissions
import io.thoth.models.LibraryPermissionsModel
import io.thoth.models.UserPermissionsModel
import io.thoth.server.database.tables.TLibraries
import io.thoth.server.database.tables.TLibraryUserMapping
import io.thoth.server.database.tables.UserPermissions
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll

fun UserPermissions.toModel(): UserPermissionsModel {
    val permissions: List<LibraryPermissionsModel>
    if (isAdmin) {
        permissions = TLibraries.selectAll().map {
            LibraryPermissionsModel(
                id = it[TLibraries.id].value,
                permissions = LibraryPermissions.READ_WRITE,
                name = it[TLibraries.name],
            )
        }
    } else {
        permissions = (TLibraryUserMapping innerJoin TLibraries)
            .select { TLibraryUserMapping.user eq id }
            .map {
                LibraryPermissionsModel(
                    id = it[TLibraryUserMapping.library].value,
                    permissions = it[TLibraryUserMapping.permissions],
                    name = it[TLibraries.name],
                )
            }
    }
    return UserPermissionsModel(
        isAdmin = isAdmin,
        libraries = permissions,
    )
}
