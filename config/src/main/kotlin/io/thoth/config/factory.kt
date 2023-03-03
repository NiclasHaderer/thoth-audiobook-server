package io.thoth.config

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceOrFileSource
import java.nio.file.Path

internal fun isProduction(): Boolean {
    return System.getenv("PRODUCTION")?.toBooleanStrictOrNull() ?: false
}

internal fun getConfigPath(): String {
    val path =
        if (isProduction()) {
            System.getenv("THOTH_CONFIG_PATH")
        } else {
            System.getenv("THOTH_CONFIG_PATH") ?: "config/preset"
        }

    return Path.of(path).toAbsolutePath().toString()
}

fun loadPublicConfig(): ThothConfig {
    val configPath = getConfigPath()

    return ConfigLoaderBuilder.default()
        .addResourceOrFileSource("$configPath/thoth-config.json")
        .build()
        .loadConfigOrThrow<ThothConfigImpl>()
}
