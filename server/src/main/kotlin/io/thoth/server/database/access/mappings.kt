package io.thoth.server.database.access

import io.thoth.models.LibraryPermissionsModel
import io.thoth.server.database.tables.LibraryUserMappingEntity

fun LibraryUserMappingEntity.toModel(): LibraryPermissionsModel =
    LibraryPermissionsModel(
        id = library.id.value,
        permissions = permissions,
        name = library.name,
    )
