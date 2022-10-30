package io.thoth.server.config

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceOrFileSource
import java.nio.file.Path

private fun isProduction(): Boolean {
    return System.getenv("PRODUCTION")?.toBooleanStrictOrNull() ?: false
}

private fun getConfigPath(): String {
    val path =  if (isProduction()) {
        System.getenv("THOTH_CONFIG_PATH")
    } else {
        System.getenv("THOTH_CONFIG_PATH") ?: "config"
    }

    return Path.of(path).toAbsolutePath().toString()
}


fun loadConfig(): ThothConfig {
    val configPath = getConfigPath()

    return ConfigLoaderBuilder.default()
        .addResourceOrFileSource("$configPath/thoth-config.yaml")
        .build()
        .loadConfigOrThrow<ThothConfigImpl>()
}
