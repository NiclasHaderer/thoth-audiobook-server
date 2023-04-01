package io.thoth.server.config

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceOrFileSource
import java.nio.file.Path

private fun isProduction(): Boolean {
    return System.getenv("PRODUCTION")?.toBooleanStrictOrNull() ?: false
}

private fun getConfigPath(): String {
    val path =
        if (isProduction()) {
            System.getenv("THOTH_CONFIG_PATH")
        } else {
            System.getenv("THOTH_CONFIG_PATH") ?: "config-preset/thoth-config.json"
        }

    return Path.of(path).toAbsolutePath().toString()
}

fun loadPublicConfig(): ThothConfig {
    val configPath = getConfigPath()

    return ConfigLoaderBuilder.default()
        .addDecoder(CronDecoder())
        .addResourceOrFileSource(configPath)
        .build()
        .loadConfigOrThrow<ThothConfigImpl>()
}
