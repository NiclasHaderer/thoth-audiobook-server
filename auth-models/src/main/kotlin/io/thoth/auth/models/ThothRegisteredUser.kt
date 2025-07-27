package io.thoth.auth.models

open class ThothRegisteredUser(open val username: String, open val passwordHash: String, open val admin: Boolean)
