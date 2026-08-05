package io.thoth.models

import java.util.UUID

data class LibraryPermissions(
    val id: UUID,
    val name: String,
    val permissions: LibraryPermissionLevel,
)
