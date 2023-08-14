package io.thoth.models

import io.thoth.auth.models.ThothUserPermissions
import java.util.*

class LibraryPermissionsModel(
    val id: UUID,
    val name: String,
    val canEdit: Boolean,
)

class UserPermissionsModel(override val isAdmin: Boolean, val libraries: List<LibraryPermissionsModel>) :
    ThothUserPermissions
