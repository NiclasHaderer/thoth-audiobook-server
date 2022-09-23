package io.thoth.server.config

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource

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

fun getConfigPath(): String {
    return if (isProduction()) {
        System.getenv("THOTH_CONFIG_PATH")
    } else {
        System.getenv("THOTH_CONFIG_PATH") ?: "thoth-config.yaml"
    }
}


fun loadConfig(): ThothConfig {
    val configFile = getConfigPath()

    return ConfigLoaderBuilder.default()
        .addResourceSource(configFile)
        .build()
        .loadConfigOrThrow<ThothConfigImpl>()
}
