package io.thoth.auth.models

interface ThothChangePassword {
    val currentPassword: String
    val newPassword: String
}

data class ThothChangePasswordImpl(
    override val currentPassword: String,
    override val newPassword: String,
) : ThothChangePassword
