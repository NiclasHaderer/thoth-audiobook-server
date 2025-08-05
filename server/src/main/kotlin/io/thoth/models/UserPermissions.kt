package io.thoth.models

import java.util.*

enum class LibraryPermissions {
    READONLY,
    READ_WRITE,
}

class LibraryPermissionsModel(val id: UUID, val name: String, val permissions: LibraryPermissions)

class UserPermissionsModel(val isAdmin: Boolean, val libraries: List<LibraryPermissionsModel>)
