package io.thoth.auth.models

interface ThothChangePassword {
    val currentPassword: String
    val newPassword: String
}
