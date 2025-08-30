package io.thoth.models

class UserPermissions(
    val isAdmin: Boolean,
    val libraries: List<LibraryPermissions>,
)
