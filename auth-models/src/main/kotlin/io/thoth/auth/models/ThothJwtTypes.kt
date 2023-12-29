package io.thoth.auth.models


enum class ThothJwtTypes(val type: String) {
    Access("access"),
    Refresh("refresh")
}
