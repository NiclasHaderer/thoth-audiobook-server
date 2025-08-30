package io.thoth.server.database.access

import io.thoth.models.LibraryPermissionsModel
import io.thoth.server.database.tables.LibraryUserEntity

fun LibraryUserEntity.toModel(): LibraryPermissionsModel =
    LibraryPermissionsModel(
        id = library.id.value,
        permissions = permissions,
        name = library.name,
    )
