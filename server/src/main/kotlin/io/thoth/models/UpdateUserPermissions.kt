package io.thoth.models

data class UpdateUserPermissions(
    val isAdmin: Boolean,
    val libraries: List<UpdateLibraryPermissions>,
)
