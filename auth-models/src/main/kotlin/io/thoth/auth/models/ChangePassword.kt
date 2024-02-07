package io.thoth.auth.models

open class ThothChangePassword(
    val currentPassword: String,
    val newPassword: String,
)
