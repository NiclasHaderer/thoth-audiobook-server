package io.thoth.models

import java.util.UUID

class UpdateLibraryPermissions(
    val id: UUID,
    val permissions: UpdatePermissions,
)
