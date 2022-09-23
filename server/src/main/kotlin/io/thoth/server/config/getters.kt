package io.thoth.server.config

fun isProduction(): Boolean {
    return System.getenv("PRODUCTION")?.toBooleanStrictOrNull() ?: false
}

fun getPort(): Int {
    return if (isProduction()) {
        System.getenv("WEB_UI_PORT").toInt()
    } else {
        System.getenv("WEB_UI_PORT")?.toIntOrNull() ?: 8080
    }
}
