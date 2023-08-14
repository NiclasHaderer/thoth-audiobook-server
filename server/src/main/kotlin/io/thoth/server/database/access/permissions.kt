package io.thoth.server.database.access

import io.thoth.models.LibraryPermissionsModel
import io.thoth.models.UserPermissionsModel
import io.thoth.server.database.tables.TLibraries
import io.thoth.server.database.tables.TLibraryUserMapping
import io.thoth.server.database.tables.UserPermissions
import org.jetbrains.exposed.sql.select

fun UserPermissions.toModel(): UserPermissionsModel {
    return UserPermissionsModel(
        isAdmin = isAdmin,
        libraries =
            (TLibraryUserMapping innerJoin TLibraries)
                .select { TLibraryUserMapping.user eq id }
                .map {
                    LibraryPermissionsModel(
                        id = it[TLibraryUserMapping.library].value,
                        canEdit = it[TLibraryUserMapping.canEdit],
                        name = it[TLibraries.name],
                    )
                },
    )
}
