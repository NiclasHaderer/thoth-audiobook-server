package io.thoth.models

import java.util.UUID

class UserPermissions(
    val isAdmin: Boolean,
    val libraries: List<LibraryPermissions>,
)

class UpdateLibraryPermissionsModel(
    val id: UUID,
    val permissions: UpdatePermissions,
)

class UpdatePermissionsModel(
    val isAdmin: Boolean,
    val libraries: List<UpdateLibraryPermissionsModel>,
)
