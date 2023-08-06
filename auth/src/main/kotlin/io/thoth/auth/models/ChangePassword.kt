package io.thoth.auth.models

interface PasswordChange {
    val currentPassword: String
    val newPassword: String
}
