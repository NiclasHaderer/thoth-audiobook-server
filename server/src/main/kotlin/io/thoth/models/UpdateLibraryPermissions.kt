package io.thoth.models

import java.util.UUID

data class UpdateLibraryPermissions(
    val id: UUID,
    val permissions: LibraryPermissionLevel,
)
