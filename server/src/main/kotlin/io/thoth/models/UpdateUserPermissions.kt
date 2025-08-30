package io.thoth.models

class UpdateUserPermissions(
    val isAdmin: Boolean,
    val libraries: List<UpdateLibraryPermissions>,
)
