package io.thoth.auth

interface ThothAuthGuard {
    fun name(): String
}

enum class T(private val _name: String) : ThothAuthGuard {
    ADMIN("ADMIN"),
    USER("USER");

    override fun name(): String = _name
}
