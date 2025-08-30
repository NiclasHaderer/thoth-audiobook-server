package io.thoth.models

import java.util.UUID

enum class LibraryPermissions {
    READONLY,
    READ_WRITE,
}

class LibraryPermissionsModel(
    val id: UUID,
    val name: String,
    val permissions: LibraryPermissions,
)

class UserPermissions(
    val isAdmin: Boolean,
    val libraries: List<LibraryPermissionsModel>,
)

class UpdateLibraryPermissionsModel(
    val id: UUID,
    val permissions: LibraryPermissions,
)

class UpdatePermissionsModel(
    val isAdmin: Boolean,
    val libraries: List<UpdateLibraryPermissionsModel>,
)
