package io.thoth.models

import java.util.UUID

class LibraryPermissions(
    val id: UUID,
    val name: String,
    val permissions: UpdatePermissions,
)
